import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

enum Type {
    NIL, NUM, SYM, ERROR, CONS, SUBR, EXPR,
}

class LObj {
    public Type tag() { return tag_; }
    public LObj(Type type, Object obj) {
        tag_ = type;
        data_ = obj;
    }
    public Integer num() {
        return (Integer)data_;
    }
    public String str() {
        return (String)data_;
    }
    public Cons cons() {
        return (Cons)data_;
    }
    public Subr subr() {
        return (Subr)data_;
    }
    public Expr exper() {
        return (Expr)data_;
    }

    public String toString() {
        if (tag_ == Type.NIL) {
            return "nil";
        } else if (tag_ == Type.NUM) {
            return num().toString();
        } else if (tag_ == Type.SYM) {
            return str();
        } else if (tag_ == Type.ERROR) {
            return "<error: " + str() + ">";
        } else if (tag_ == Type.CONS) {
            return listToString(this);
        } else if (tag_ == Type.SUBR) {
            return "<subr>";
        } else if (tag_ == Type.EXPR) {
            return "<expr>";
        }
        return "<unknown>";
    }

    private String listToString(LObj obj) {
        String ret = "";
        boolean first = true;
        while (obj.tag() == Type.CONS) {
            if (first) {
                first = false;
            } else {
                ret += " ";
            }
            ret += obj.cons().car.toString();
            obj = obj.cons().cdr;
        }
        if (obj.tag() == Type.NIL) {
            return "(" + ret + ")";
        }
        return "(" + ret + " . " + obj.toString() + ")";
    }

    private Type tag_;
    private Object data_;
}

class Cons {
    public Cons(LObj a, LObj d) {
        car = a;
        cdr = d;
    }
    public LObj car;
    public LObj cdr;
}

interface Subr {
    LObj call(LObj args);
}

class Expr {
    public Expr(LObj a, LObj b, LObj e) {
        args = a;
        body = b;
        env = e;
    }
    public LObj args;
    public LObj body;
    public LObj env;
}

class Util {
    public static LObj makeNum(Integer num) {
        return new LObj(Type.NUM, num);
    }
    public static LObj makeError(String str) {
        return new LObj(Type.ERROR, str);
    }
    public static LObj makeCons(LObj a, LObj d) {
        return new LObj(Type.CONS, new Cons(a, d));
    }
    public static LObj makeSubr(Subr subr) {
        return new LObj(Type.SUBR, subr);
    }
    public static LObj makeExpr(LObj args, LObj env) {
        return new LObj(Type.EXPR, new Expr(safeCar(args), safeCdr(args), env));
    }
    public static LObj makeSym(String str) {
        if (str.equals("nil")) {
            return kNil;
        } else if (!symbolMap.containsKey(str)) {
            symbolMap.put(str, new LObj(Type.SYM, str));
        }
        return symbolMap.get(str);
    }

    public static LObj safeCar(LObj obj) {
        if (obj.tag() == Type.CONS) {
            return obj.cons().car;
        }
        return kNil;
    }
    public static LObj safeCdr(LObj obj) {
        if (obj.tag() == Type.CONS) {
            return obj.cons().cdr;
        }
        return kNil;
    }

    public static LObj nreverse(LObj lst) {
        LObj ret = kNil;
        while (lst.tag() == Type.CONS) {
            LObj tmp = lst.cons().cdr;
            lst.cons().cdr = ret;
            ret = lst;
            lst = tmp;
        }
        return ret;
    }

    public final static LObj kNil = new LObj(Type.NIL, "nil");
    private static Map<String, LObj> symbolMap = new HashMap<String, LObj>();
}

class ParseState {
    public ParseState(LObj o, String s) {
        obj = o;
        next = s;
    }
    public LObj obj;
    public String next;
}

class Reader {
    private static boolean isSpace(char c) {
        return c == '\t' || c == '\r' || c == '\n' || c == ' ';
    }

    private static boolean isDelimiter(char c) {
        return c == kLPar || c == kRPar || c == kQuote || isSpace(c);
    }

    private static String skipSpaces(String str) {
        int i;
        for (i = 0; i < str.length(); i++) {
            if (!isSpace(str.charAt(i))) {
                break;
            }
        }
        return str.substring(i, str.length());
    }

    private static LObj makeNumOrSym(String str) {
        try {
            return Util.makeNum(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return Util.makeSym(str);
        }
    }

    private static ParseState parseError(String s) {
        return new ParseState(Util.makeError(s), "");
    }

    private static ParseState readAtom(String str) {
        String next = "";
        for (int i = 0; i < str.length(); i++) {
            if (isDelimiter(str.charAt(i))) {
                next = str.substring(i, str.length());
                str = str.substring(0, i);
                break;
            }
        }
        return new ParseState(makeNumOrSym(str), next);
    }

    public static ParseState read(String str) {
        str = skipSpaces(str);
        if (str.length() == 0) {
            return parseError("empty input");
        } else if (str.charAt(0) == kRPar) {
            return parseError("invalid syntax: " + str);
        } else if (str.charAt(0) == kLPar) {
            return readList(str.substring(1, str.length()));
        } else if (str.charAt(0) == kQuote) {
            ParseState tmp = read(str.substring(1, str.length()));
            return new ParseState(
                Util.makeCons(Util.makeSym("quote"),
                              Util.makeCons(tmp.obj, Util.kNil)),
                tmp.next);
        }
        return readAtom(str);
    }

    private static ParseState readList(String str) {
        LObj ret = Util.kNil;
        while (true) {
            str = skipSpaces(str);
            if (str.length() == 0) {
                return parseError("unfinished parenthesis");
            } else if (str.charAt(0) == kRPar) {
                break;
            }
            ParseState tmp = read(str);
            if (tmp.obj.tag() == Type.ERROR) {
                return tmp;
            }
            ret = Util.makeCons(tmp.obj, ret);
            str = tmp.next;
        }
        return new ParseState(Util.nreverse(ret),
                              str.substring(1, str.length()));
    }

    private final static char kLPar = '(';
    private final static char kRPar = ')';
    private final static char kQuote = '\'';

}

class Evaluator {
    private static LObj findVar(LObj sym, LObj env) {
        while (env.tag() == Type.CONS) {
            LObj alist = env.cons().car;
            while (alist.tag() == Type.CONS) {
                if (alist.cons().car.cons().car == sym) {
                    return alist.cons().car;
                }
                alist = alist.cons().cdr;
            }
            env = env.cons().cdr;
        }
        return Util.kNil;
    }

    public static void addToEnv(LObj sym, LObj val, LObj env) {
        env.cons().car = Util.makeCons(Util.makeCons(sym, val), env.cons().car);
    }

    public static LObj eval(LObj obj, LObj env) {
        if (obj.tag() == Type.NIL || obj.tag() == Type.NUM ||
            obj.tag() == Type.ERROR) {
            return obj;
        } else if (obj.tag() == Type.SYM) {
            LObj bind = findVar(obj, env);
            if (bind == Util.kNil) {
                return Util.makeError(obj.str() + " has no value");
            }
            return bind.cons().cdr;
        }
        return Util.makeError("noimpl");
    }

    public static LObj makeGlobalEnv() {
        LObj env = Util.makeCons(Util.kNil, Util.kNil);
        addToEnv(Util.makeSym("t"), Util.makeSym("t"), env);
        return env;
    }
}

public class Lisp {
    public static void main(String[] args) {
        InputStreamReader ireader = new InputStreamReader(System.in);
        BufferedReader breader = new BufferedReader(ireader);
        LObj gEnv = Evaluator.makeGlobalEnv();
        try {
            String line;
            System.out.print("> ");
            while ((line = breader.readLine()) != null) {
                System.out.print(Evaluator.eval(Reader.read(line).obj, gEnv));
            System.out.print("\n> ");
            }
        } catch (IOException e) {}
    }
}
