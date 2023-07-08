import rclang.lexer.Lexer
import rclang.parser.RcParser
import rclang.ast.*
import rclang.compiler

import java.io.File
import java.net.URI
import rclang.tools.{DumpManager, GlobalTable, unwrap}
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import rclang.ast.Expr.Self

import scala.collection.immutable

def driverByPath(path: String) = {
  // 1. read from uri
  val f = new File(path)
  val source = scala.io.Source.fromFile(f)
  val src = source.getLines().mkString("\n") + "\n"
  // 2. parse and return ast
  val ast = RcParser(Lexer(src).unwrap).unwrap
  ast
}

def driver(uri: String) = {
  // 1. read from uri
  val f = new File(URI.create(uri))
  val source = scala.io.Source.fromFile(f)
  val src = source.getLines().mkString("\n") + "\n"
  // 2. parse and return ast
  val ast = RcParser(Lexer(src).unwrap).unwrap
  ast
}

def build(uri: String, client: LanguageClient) = {
  client.logTrace(new LogTraceParams("build", s"build $uri"))
  DumpManager.setDumpRoot("/Users/homura/Code/Rc-lang-Language-Server/tmp")
  compiler.Driver(compiler.CompileOption(List(uri), "/Users/homura/Code/Rc-lang-Language-Server/a.out"))
}

def symbolTable(uri: String): GlobalTable = {
  val ast = driver(uri)
  symbolTable(ast)
}

class RcContext() {
  var ast: RcModule = null
  var table: GlobalTable = null
  var tree: ASTNodeTree = null
  def init(uri: String) = {
    ast = driver(uri)
    table = symbolTable(ast)
    tree = new TreeBuilder().build(ast)
  }
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

class PositionNode(var parents: List[TreeNode] = Nil) {
  def currentNode = if parents.isEmpty then None else Some(parents.maxBy(_.node.priority))
  def position = currentNode.map(_.position)
}

def getPositionNode(ast: RcModule, position: Position, context: RcContext): PositionNode = {
  val list = getNode(ast, position)
  PositionNode(list.map(context.tree(_)))
}

def astToStrInHover(value: ASTNode): String = {
    value match
      case klass: rclang.ast.Class => s"class ${klass.name.str}"
      case method: rclang.ast.Method => s"method ${method.name.str}"
      case _ => s"${value.getClass.getSimpleName}: ${value.toString}"
}

class ASTPrinter(var nest: Boolean = true) extends rclang.ast.ASTVisitor {
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
    if(!nest) {
      return
    }
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
    writeLine(expr.getClass.getSimpleName, expr)
    doIndent(() => {
      expr match
        case Expr.Identifier(ident) => visit(ident)
        case Expr.Binary(op, lhs, rhs) => visit(lhs); visit(rhs)
        case Expr.If(cond, true_branch, false_branch) => {
          visit(cond)
          visit(true_branch)
          false_branch.map(visit)
        }
        case Expr.Lambda(args, block) => {
          args.params.foreach(visit)
          visit(block)
        }
        case Expr.Call(target, args) => {
          visit(target)
          args.foreach(visit)
        }
        case Expr.MethodCall(obj, target, args) => {
          visit(obj)
          visit(target)
          args.foreach(visit)
        }
        case Expr.Block(stmts) => {
          stmts.foreach(visit)
        }
        case Expr.Return(expr) => visit(expr)
        case Expr.Field(expr, ident) => {
          visit(expr)
          visit(ident)
        }
        case Expr.Symbol(ident) => visit(ident)
        case Expr.Index(expr, i) => {
          visit(expr)
          visit(i)
        }
        case Expr.Array(len, initValues) => {
          initValues.foreach(visit)
        }
        case _ =>
    })
  }

  override def visit(stmt: Stmt): R = {
    writeLine("stmt", stmt)
    doIndent(() => {
      stmt match
        case Stmt.Local(name, tyInfo, value) => {
          visit(name)
          visit(tyInfo)
          visit(value)
        }
        case Stmt.Expr(expr) => visit(expr)
        case Stmt.While(cond, body) => {
          visit(cond)
          visit(body)
        }
        case Stmt.For(init, cond, incr, body) => {
          visit(init)
          visit(cond)
          visit(incr)
          visit(body)
        }
        case Stmt.Assign(name, value) => {
          visit(name)
          visit(value)
        }
        case Stmt.Break() => writeLine("break", stmt)
        case Stmt.Continue() => writeLine("continue", stmt)
    })
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
      val tree = new ASTNodeTree()
      val list = tree(expr).children.map(_.node).flatMap {op => {
        op match
          case expr: Expr => searchExpr(op.asInstanceOf[Expr])
          case _ => List()
      }}
      list
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


class TreeNode(var node: ASTNode, var children: List[TreeNode], var parent: TreeNode = null) {
  children.foreach(_.parent = this)

  def position = node.getPosition

  def positionRange = node.getPositionRange

  def label = {
    val p = new ASTPrinter(false)
    node match
      case expr: Expr => p.visit(expr)
      case field: FieldDef => p.visit(field)
      case ident: Ident => p.visit(ident)
      case item: Item => p.visit(item)
      case methodDecl: MethodDecl => p.visit(methodDecl)
      case modules: Modules => p.visit(modules)
      case param: Param => p.visit(param)
      case params: Params => params.params.foreach(p.visit)
      case rcModule: RcModule => p.visit(rcModule)
      case stmt: Stmt => p.visit(stmt)
      case info: TyInfo => p.visit(info)
      case _ => "notSupported"
    p.result
  }
}

class ASTNodeTree {
  var root: TreeNode = null
  private var astNodeMap = Map[ASTNode, TreeNode]()
  private var strNodeMap = Map[String, TreeNode]()
  private var nodes = Set[TreeNode]()

  def apply(node: ASTNode): TreeNode = astNodeMap(node)
  def apply(str: String): TreeNode = strNodeMap(str)

  def addNode(node: TreeNode): Unit = {
    astNodeMap = astNodeMap + (node.node -> node)
    strNodeMap = strNodeMap + (node.label -> node)
    nodes = nodes + node
  }
}

class TreeBuilder {
  type R = TreeNode
  val root = new ASTNodeTree()

  private def newTreeNode(node: ASTNode, children: List[TreeNode]) = {
    val newNode: TreeNode = new TreeNode(node, children)
    root.addNode(newNode)
    newNode
  }

  def build(module: RcModule) = {
    val node = visit(module)
    root.root = node
    root
  }

  def visit(module: RcModule): R = {
    // todo: set root
    val children = module.items.map(visit)
    newTreeNode(module, children)

  }

  def visit(item: Item): R = {
    item match
      case m @ Method(decl, body) => visit(m)
      case c @ Class(name, parent, vars, methods, generic) => visit(c)
      case _ => ???
  }

  def visit(expr: Expr): R = {
    val child: List[R] = expr match
      // todo: visit op
      case Expr.Identifier(ident) => List(ident).map(visit)
      case Expr.Binary(op, lhs, rhs) => List(visit(lhs), visit(rhs))
      case Expr.If(cond, true_branch, false_branch) => {
        val falseBr = false_branch match
          case Some(value) => List(visit(value))
          case None => Nil
        List(visit(cond), visit(true_branch)):::falseBr
      }
      case Expr.Lambda(args, block) => args.params.map(visit):::List(visit(block))
      case Expr.Call(target, args) => List(visit(target)):::args.map(visit)
      case Expr.MethodCall(obj, target, args) => List(visit(obj), visit(target)):::args.map(visit)
      case Expr.Block(stmts) => stmts.map(visit)
      case Expr.Return(expr) => List(expr).map(visit)
      case Expr.Field(expr, ident) => List(visit(expr), visit(ident))
      case Expr.Symbol(ident) => List(ident).map(visit)
      case Expr.Index(expr, i) => List(expr).map(visit)
      case Expr.Array(len, initValues) => initValues.map(visit)
      case _ => List()
    newTreeNode(expr, child)
  }

  def visit(stmt: Stmt): R = {
    val child = stmt match
      case Stmt.Local(name, tyInfo, value) => List(visit(name), visit(tyInfo), visit(value))
      case Stmt.Expr(expr) => List(expr).map(visit)
      case Stmt.While(cond, body) => List(cond, body).map(visit)
      case Stmt.For(init, cond, incr, body) => List(visit(init), visit(cond), visit(incr), visit(body))
      case Stmt.Assign(name, value) => List(visit(value))
      case _ => Nil
    newTreeNode(stmt, child)
  }

  def visit(ty: TyInfo): R = {
    newTreeNode(ty, Nil)
  }

  def visit(decl: MethodDecl): R = {
    newTreeNode(decl, Nil)
  }

  def visit(ident: Ident): R = {
    newTreeNode(ident, Nil)
  }

   def visit(param: Param): R = {
     newTreeNode(param, Nil)
   }

   def visit(field: FieldDef): R = {
     newTreeNode(field, Nil)
   }

  def visit(method: Method): R = {
    val child = List(visit(method.name), visit(method.decl), visit(method.body))
    newTreeNode(method, child)
  }

  def visit(klass: Class): R = {
    val varNodes = klass.vars.map(visit)
    val methodNodes = klass.methods.map(visit)
    val child = List(visit(klass.name)):::varNodes:::methodNodes
    newTreeNode(klass, child)
  }
}


//def children(uri) {
//  tree(uri).children.map(child => {
//    val label = child.label
//    new TreeNodeInfo(label, ServerCommands.GoTo)
//    )
//  }
//}
