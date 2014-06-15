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
    public Expr expr() {
        return (Expr)data_;
    }

    @Override public String toString() {
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

class Subr {
    public LObj call(LObj args) { return args; }
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

    public static LObj pairlis(LObj lst1, LObj lst2) {
        LObj ret = kNil;
        while (lst1.tag() == Type.CONS && lst2.tag() == Type.CONS) {
            ret = makeCons(makeCons(lst1.cons().car, lst2.cons().car), ret);
            lst1 = lst1.cons().cdr;
            lst2 = lst2.cons().cdr;
        }
        return nreverse(ret);
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

        LObj op = Util.safeCar(obj);
        LObj args = Util.safeCdr(obj);
        if (op == Util.makeSym("quote")) {
            return Util.safeCar(args);
        } else if (op == Util.makeSym("if")) {
            if (eval(Util.safeCar(args), env) == Util.kNil) {
                return eval(Util.safeCar(Util.safeCdr(Util.safeCdr(args))),
                            env);
            }
            return eval(Util.safeCar(Util.safeCdr(args)), env);
        } else if (op == Util.makeSym("lambda")) {
            return Util.makeExpr(args, env);
        } else if (op == Util.makeSym("defun")) {
            LObj expr = Util.makeExpr(Util.safeCdr(args), env);
            LObj sym = Util.safeCar(args);
            addToEnv(sym, expr, gEnv);
            return sym;
        } else if (op == Util.makeSym("setq")) {
            LObj val = eval(Util.safeCar(Util.safeCdr(args)), env);
            LObj sym = Util.safeCar(args);
            LObj bind = findVar(sym, env);
            if (bind == Util.kNil) {
                addToEnv(sym, val, gEnv);
            } else {
                bind.cons().cdr = val;
            }
            return val;
        }
        return apply(eval(op, env), evlis(args, env), env);
    }

    private static LObj evlis(LObj lst, LObj env) {
        LObj ret = Util.kNil;
        while (lst.tag() == Type.CONS) {
            LObj elm = eval(lst.cons().car, env);
            if (elm.tag() == Type.ERROR) {
                return elm;
            }
            ret = Util.makeCons(elm, ret);
            lst = lst.cons().cdr;
        }
        return Util.nreverse(ret);
    }

    private static LObj progn(LObj body, LObj env) {
        LObj ret = Util.kNil;
        while (body.tag() == Type.CONS) {
            ret = eval(body.cons().car, env);
            body = body.cons().cdr;
        }
        return ret;
    }

    private static LObj apply(LObj fn, LObj args, LObj env) {
        if (fn.tag() == Type.ERROR) {
            return fn;
        } else if (args.tag() == Type.ERROR) {
            return args;
        } else if (fn.tag() == Type.SUBR) {
            return fn.subr().call(args);
        } else if (fn.tag() == Type.EXPR) {
            return progn(fn.expr().body,
                         Util.makeCons(Util.pairlis(fn.expr().args, args),
                                       fn.expr().env));
        }
        return Util.makeError(fn.toString() + " is not function");
    }

    private static LObj makeGlobalEnv() {
        Subr subrCar = new Subr() {
                @Override public LObj call(LObj args) {
                    return Util.safeCar(Util.safeCar(args));
                }
        };
        Subr subrCdr = new Subr() {
                @Override public LObj call(LObj args) {
                    return Util.safeCdr(Util.safeCar(args));
                }
        };
        Subr subrCons = new Subr() {
                @Override public LObj call(LObj args) {
                    return Util.makeCons(Util.safeCar(args),
                                         Util.safeCar(Util.safeCdr(args)));
                }
        };
        Subr subrEq = new Subr() {
                @Override public LObj call(LObj args) {
                    LObj x = Util.safeCar(args);
                    LObj y = Util.safeCar(Util.safeCdr(args));
                    if (x.tag() == Type.NUM && y.tag() == Type.NUM) {
                        if (x.num() == y.num()) {
                            return Util.makeSym("t");
                        } else {
                            return Util.kNil;
                        }
                    } else if (x == y) {
                        return Util.makeSym("t");
                    }
                    return Util.kNil;
                }
        };
        Subr subrAtom = new Subr() {
                @Override public LObj call(LObj args) {
                    if (Util.safeCar(args).tag() == Type.CONS) {
                        return Util.kNil;
                    }
                    return Util.makeSym("t");
                }
        };

        Subr subrNumberp = new Subr() {
                @Override public LObj call(LObj args) {
                    if (Util.safeCar(args).tag() == Type.NUM) {
                        return Util.makeSym("t");
                    }
                    return Util.kNil;
                }
        };
        Subr subrSymbolp = new Subr() {
                @Override public LObj call(LObj args) {
                    if (Util.safeCar(args).tag() == Type.SYM) {
                        return Util.makeSym("t");
                    }
                    return Util.kNil;
                }
        };
        class SubrAddOrMul extends Subr {
            @Override public LObj call(LObj args) {
                Integer ret = initVal;
                while (args.tag() == Type.CONS) {
                    if (args.cons().car.tag() != Type.NUM) {
                        return Util.makeError("wrong type");
                    }
                    ret = calc(ret, args.cons().car.num());
                    args = args.cons().cdr;
                }
                return Util.makeNum(ret);
            }
            public SubrAddOrMul(Integer i) {
                initVal = i;
            }
            public Integer calc(Integer x, Integer y) { return 0; }
            private Integer initVal;
        }
        Subr subrAdd = new SubrAddOrMul(0) {
                @Override public Integer calc(Integer x, Integer y) {
                    return x + y;
                }
        };
        Subr subrMul = new SubrAddOrMul(1) {
                @Override public Integer calc(Integer x, Integer y) {
                    return x * y;
                }
        };
        class SubrSubOrDivOrMod extends Subr {
            @Override public LObj call(LObj args) {
                LObj x = Util.safeCar(args);
                LObj y = Util.safeCar(Util.safeCdr(args));
                if (x.tag() != Type.NUM || y.tag() != Type.NUM) {
                    return Util.makeError("wrong type");
                }
                return Util.makeNum(calc(x.num(), y.num()));
            }
            public Integer calc(Integer x, Integer y) { return 0; }
        }
        Subr subrSub = new SubrSubOrDivOrMod() {
                @Override public Integer calc(Integer x, Integer y) {
                    return x - y;
                }
        };
        Subr subrDiv = new SubrSubOrDivOrMod() {
                @Override public Integer calc(Integer x, Integer y) {
                    return x / y;
                }
        };
        Subr subrMod = new SubrSubOrDivOrMod() {
                @Override public Integer calc(Integer x, Integer y) {
                    return x % y;
                }
        };

        LObj env = Util.makeCons(Util.kNil, Util.kNil);
        addToEnv(Util.makeSym("car"), Util.makeSubr(subrCar), env);
        addToEnv(Util.makeSym("cdr"), Util.makeSubr(subrCdr), env);
        addToEnv(Util.makeSym("cons"), Util.makeSubr(subrCons), env);
        addToEnv(Util.makeSym("eq"), Util.makeSubr(subrEq), env);
        addToEnv(Util.makeSym("atom"), Util.makeSubr(subrAtom), env);
        addToEnv(Util.makeSym("numberp"), Util.makeSubr(subrNumberp), env);
        addToEnv(Util.makeSym("symbolp"), Util.makeSubr(subrSymbolp), env);
        addToEnv(Util.makeSym("+"), Util.makeSubr(subrAdd), env);
        addToEnv(Util.makeSym("*"), Util.makeSubr(subrMul), env);
        addToEnv(Util.makeSym("-"), Util.makeSubr(subrSub), env);
        addToEnv(Util.makeSym("/"), Util.makeSubr(subrDiv), env);
        addToEnv(Util.makeSym("mod"), Util.makeSubr(subrMod), env);
        addToEnv(Util.makeSym("t"), Util.makeSym("t"), env);
        return env;
    }

    public static LObj globalEnv() { return gEnv; }

    private static LObj gEnv = makeGlobalEnv();
}

public class Lisp {
    public static void main(String[] args) {
        InputStreamReader ireader = new InputStreamReader(System.in);
        BufferedReader breader = new BufferedReader(ireader);
        LObj gEnv = Evaluator.globalEnv();
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
