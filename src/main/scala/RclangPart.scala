import rclang.lexer.Lexer
import rclang.parser.RcParser
import rclang.ast.*

import java.io.File
import java.net.URI
import rclang.tools.{DumpManager, GlobalTable, unwrap}
import org.eclipse.lsp4j.*
import rclang.ast.Expr.Self

import scala.collection.immutable

def driver(uri: String) = {
  // 1. read from uri
  val f = new File(URI.create(uri))
  val source = scala.io.Source.fromFile(f)
  val src = source.getLines().mkString("\n") + "\n"
  // 2. parse and return ast
  val ast = RcParser(Lexer(src).unwrap).unwrap
  ast
}

def symbolTable(uri: String): GlobalTable = {
  val ast = driver(uri)
  symbolTable(ast)
}

def symbolTable(ast: RcModule): GlobalTable = {
  rclang.compiler.Driver.typeProc(ast)._2
}

def initRclang() = {
  DumpManager.mkDumpRootDir
}

def keywords = rclang.lexer.Keyword.values

// position to ast node
def getNode(ast: RcModule, position: Position) = {
  val searcher = new NodeSearcher(position)
  searcher.searchModule(ast)
}

class ASTPrinter extends rclang.ast.ASTVisitor {
  var result = ""
  val indent = "\t"
  var level = 0;
  var end = true

  def IndentStr = {
    if end then indent * level else ""
  }
  def write(s: String): Unit = {
    result += IndentStr + s
    end = false
  }

  def writeLine(s: String, node: ASTNode) = {
    result += IndentStr + s + s"<${node.getPositionRange.simplyStr}>"+ "\n"
    end = true
  }

  def doIndent(f: () => Unit): Unit = {
    level += 1
    f()
    level -= 1
  }

  override def visit(modules: Modules): R = {
    modules.modules.map(visit).mkString("\n")
  }

  override def visit(module: RcModule): R = {
    writeLine(s"RcModule ${module.name}", module)
    doIndent(() => {
      module.items.map(visit)
    })
  }

  override def visit(method: Method): R = {
    writeLine(s"Method ${method.decl.name.str}", method)
    doIndent(() => {
      visit(method.decl)
      visit(method.body)
    })
  }

  override def visit(klass: Class): R = {
    writeLine(s"Class ${klass.name}", klass)
    doIndent(() => {
      klass.parent match
        case Some(parent) => write("Parent:");visit(parent)
        case None =>
      klass.vars.map(visit)
      klass.methods.map(visit)
      klass.generic match
        case Some(generic) => visit(generic)
        case None =>
    })
  }

  override def visit(expr: Expr): R = {
    writeLine("expr", expr)
  }

  override def visit(stmt: Stmt): R = {
    writeLine(stmt.getClass.getSimpleName, stmt)
  }

  override def visit(ty: TyInfo): R = writeLine(s"TyInfo ${ty.getClass.getSimpleName}", ty)

  override def visit(decl: MethodDecl): R = {
    writeLine(s"MethodDecl ${decl.name.str}", decl)
    doIndent(() => {
      decl.inputs.params.map(visit)
      visit(decl.outType)
    })
  }

  override def visit(ident: Ident): R = writeLine(s"Ident ${ident.str}", ident)

  override def visit(param: Param): R = writeLine(s"Param ${param.name.str}", param)

  override def visit(field: FieldDef): R = writeLine(s"FieldDef ${field.name.str}", field)
}


class NodeSearcher(val position: Position) {
  def searchModule(ast: RcModule): List[ASTNode] = {
    ast.items.flatMap(searchItem)
  }

  def searchItem(ast: Item): List[ASTNode] = {
    ast match
      case m@Method(decl, body) => searchMethod(m)
      case c@Class(name, parent, vars, methods, generic) => searchClass(c)
      case _ => List()
  }

  def searchMethod(ast: Method): List[ASTNode] = {
    ast.decl.containsPosition(position) match
      case true => List(ast.decl, ast)
      case false => {
        val body = searchExpr(ast.body)
        if body.isEmpty then body else body.appended(ast)
      }
  }

  def searchClass(klass: rclang.ast.Class): List[ASTNode] = {
    var list = List[ASTNode](klass)
    list = klass.parent match
      case Some(parent) => list :+ parent
      case None => list
    list = list.concat(klass.vars).concat(klass.methods)
    list = klass.generic match
      case Some(generic) => list :+ generic
      case None => list
    val resultList = list.filter(ast => {
      ast.containsPosition(position)
    })
    if resultList.isEmpty then resultList else resultList
  }

  def searchExpr(expr: Expr): List[ASTNode] = {
    if(!expr.containsPosition(position)) {
      List()
    } else {
      List(expr)
//      val list = expr.operands.map{op => {
//        op match
//          case expr@Expr => searchExpr(expr.asInstanceOf[Expr])
//          case _ => Some(op)
//      }}
//      list
    }
  }
}

def astToStr(ast: RcModule) = {
  val p = new ASTPrinter()
  p.visit(ast)
  p.result
}

extension (r: Range) {
  def contains(position: Position): Boolean = {
    val line = position.getLine
    val col = position.getCharacter
    val start = r.getStart
    val end = r.getEnd
    start.getLine < line && line < end.getLine ||
      (start.getLine == line && start.getCharacter <= col) ||
      (end.getLine == line && end.getCharacter >= col)
  }

  def simplyStr: String = {
    s"${r.getStart.getLine}:${r.getStart.getCharacter}, ${r.getEnd.getLine}:${r.getEnd.getCharacter}"
  }
}

extension (p: ASTNode) {
  def getPosition = new Position(p.pos.line - 1, p.pos.column - 1)

  def getPositionRange: Range = p match
    case method: Method => PositionRange.getMethodPositionRange(method)
    case fieldDef: FieldDef => PositionRange.getFieldPositionRange(fieldDef)
    case klass: rclang.ast.Class => PositionRange.getClassPositionRange(klass)
    case _ => new Range(p.getPosition, p.getPosition)

  def containsPosition(position: Position) = p.getPositionRange.contains(position)

  def priority = {
    p match
      case _: rclang.ast.Class => 1
      case _: Method => 2
      case _ => 0
  }
}

object PositionRange {
  def getMethodPositionRange(method: Method) = {
    val begin = method.name.getPosition
    if (method.body.stmts.isEmpty) {
      new Range(begin, begin)
    } else {
      val end = method.body.stmts.last.getPosition
      new Range(begin, end)
    }
  }

  def getFieldPositionRange(fieldDef: FieldDef) =
    new Range(fieldDef.name.getPosition, fieldDef.getPosition)

  def getClassPositionRange(klass: rclang.ast.Class) = {
    val symbols = klass.methods.concat(klass.vars)
    val begin = klass.name.getPosition
    if (symbols.isEmpty) {
      new Range(begin, begin)
    }
    else {
      val end = getMethodPositionRange(klass.methods.maxBy(_.name.pos.line)).getEnd
      new Range(begin, end)
    }
  }
}