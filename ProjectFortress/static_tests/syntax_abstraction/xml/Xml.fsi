(* Xml grammar for writing inline xml. *)

api Xml

  import FortressAst.{...}
  import FortressSyntax.{Literal}

  grammar xml extends Literal
    LiteralExpr |Expr:=
      xml x:XExpr <[ x ]>

    XExpr :Expr:=
      b:XmlStart c:XmlContent e:XmlEnd
      <[ "<" b ">" c "</" e ">" ]>
    | x:XmlComplete <[ "<" x "/>" ]>

    XmlComplete :Expr:=
      OpenBracket# s:String Slash# CloseBracket
      <[ s ]>
    | OpenBracket# s:String a:Attributes Slash# CloseBracket
      <[ s " " a ]>

    Attributes :Expr:=
      a:Attribute r:Attributes <[ a " " r ]>
    | a:Attribute <[ a ]>

    Attribute :Expr:=
      key:String = " val:Strings " <[ key "='" val "'" ]>

    XmlStart :Expr:=
      o1:OpenBracket# s:String o2:CloseBracket
      <[ s ]>
    | o1:OpenBracket# s:String a:Attributes o2:CloseBracket
      <[ s " " a ]>

    XmlContent :Expr:=
      s:Strings <[ s ]>
    | x:XExpr <[ x ]>

    Strings :Expr:=
      s1:String s2:Strings <[ s1 " " s2 ]>
    | s1:String <[ s1 ]>

    XmlEnd :Expr:=
      o1:OpenBracket# Slash# s:String# o2:CloseBracket
      <[ s ]>

    Slash :StringLiteralExpr:=
      / <[ "/" ]>

    String :Expr:=
      x:AnyChar# y:String <[ x y ]>
    | x:AnyChar <[ x "" ]>

    (* There must be a simpler way to do this *)
    AnyChar :StringLiteralExpr:=
      x:a <[ x ]>
    | x:b <[ x ]>
    | x:c <[ x ]>
    | x:d <[ x ]>
    | x:e <[ x ]>
    | x:f <[ x ]>
    | x:g <[ x ]>
    | x:h <[ x ]>
    | x:i <[ x ]>
    | x:k <[ x ]>
    | x:l <[ x ]>
    | x:m <[ x ]>
    | x:n <[ x ]>
    | x:o <[ x ]>
    | x:p <[ x ]>
    | x:q <[ x ]>
    | x:r <[ x ]>
    | x:s <[ x ]>
    | x:t <[ x ]>
    | x:u <[ x ]>
    | x:v <[ x ]>
    | x:w <[ x ]>
    | x:x <[ x ]>
    | x:y <[ x ]>
    | x:z <[ x ]>

    (* Shouldn't need [] around < and > *)
    OpenBracket :Expr:=
      [<] <[ "<" ]>

    CloseBracket :Expr:=
      [>] <[ ">" ]>

  end
end
