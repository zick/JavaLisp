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
            return parseError("noimpl");
        } else if (str.charAt(0) == kQuote) {
            return parseError("noimpl");
        }
        return readAtom(str);
    }

    private final static char kLPar = '(';
    private final static char kRPar = ')';
    private final static char kQuote = '\'';

}

public class Lisp {
    public static void main(String[] args) {
        InputStreamReader ireader = new InputStreamReader(System.in);
        BufferedReader breader = new BufferedReader(ireader);
        try {
            String line;
            System.out.print("> ");
            while ((line = breader.readLine()) != null) {
                System.out.print(Reader.read(line).obj);
            System.out.print("\n> ");
            }
        } catch (IOException e) {}
    }
}
