import rclang.lexer.Lexer
import rclang.parser.RcParser
import rclang.ast.*
import rclang.compiler

import java.io.File
import java.net.URI
import rclang.tools.{DumpManager, FullName, GlobalTable, unwrap}
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import rclang.ast.Expr.Self
import rclang.ast.TyInfo.Infer

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
  var client: LanguageClient = null
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
def getNode(ast: RcModule, position: Position)(using context: RcContext) = {
  val searcher = new NodeSearcher(position, context)
  searcher.searchModule(ast)
}

class PositionNode(var parents: List[TreeNode] = Nil, var originPosition: Position) {
  var client = null: LanguageClient
  private def getPriority(range: Range): Int = {
    // 行范围最小的
    val line = Math.abs(range.getEnd.getLine - range.getStart.getLine)
    // 行最接近position的
    val character = Math.abs(range.getStart.getCharacter - originPosition.getCharacter)
    // 但是如果有多个同样起点的，就选择最短的
    val size = Math.abs(range.getEnd.getCharacter - range.getStart.getCharacter)
    client.logMessage(new MessageParams(MessageType.Info, s"getPriority $line $character"))
    // character相同的情况下看size
    val result = line * 1000 + character + size
    client.logMessage(new MessageParams(MessageType.Info, s"$result"))
    result
  }

  def currentNode = {
    if (parents.isEmpty) {
      None
    } else {
      val msg = parents.map(parent => s"${parent.node.getClass} ${getPriority(parent.node.getPositionRange)} ${parent.node}").mkString("\n")
      val result = parents.minBy(parent => getPriority(parent.node.getPositionRange))
      Some(result)
    }
  }
  def position = currentNode.map(_.position)
}

def getPositionNode(ast: RcModule, position: Position)(using context: RcContext): PositionNode = {
  val list = getNode(ast, position)
  PositionNode(list.map(context.tree(_)), position)
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

def makeFullName(node: TreeNode) = {
//  FullName()
}

def lookup(name: String)(using context: RcContext): Option[ASTNode] = {
  // Symbol -> Class
  if(name.head.isUpper) {
    context.table.classTable.get(name).map(_.astNode)
  } else {
    None
    // local var
    // field
    // method
  }
}

def definitionList(node: TreeNode)(using context: RcContext): List[ASTNode] = {
  node.node match
    case Ident(name) => {
      lookup(name) match
        case Some(value) => List(value)
        case None => {
          context.client.logMessage(new MessageParams(MessageType.Info, "DefNone"))
          List()
        }
    }
    case _ => {
      context.client.logMessage(new MessageParams(MessageType.Info, "Expr Not Expr"))
      context.client.logMessage(new MessageParams(MessageType.Info, node.node.toString))
      List()
    }
}

class NodeSearcher(val position: Position, val context: RcContext) {
  def search(astNode: ASTNode): List[ASTNode] = {
    astNode match
      case Empty => List()
      case expr: Expr => searchExpr(expr)
//      case FieldDef(name, ty, initValue) =>
      case id: Ident => {
        if id.getPositionRange.contains(position) then
          List(id)
        else
          List()
      }
      case item: Item => searchItem(item)
//      case MethodDecl(name, inputs, outType, generic) =>
//      case Modules(modules) =>
      case Param(name, ty) => search(name):::search(ty)
      case Params(params) => params.flatMap(search)
      case RcModule(items, name, refs) => items.flatMap(search)
      case stmt: Stmt => searchStmt(stmt)
      case info: TyInfo => searchTyInfo(info)
      case _ => List()
  }

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
    if(ast.decl.name.containsPosition(position)) {
      return List(ast.decl.name)
    }
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
    if (!expr.containsPosition(position)) {
//      context.client.logMessage(new MessageParams(MessageType.Warning, s"expr ${expr.getClass.getSimpleName} not contains position ${position.getLine}:${position.getCharacter}"))
      List()
    } else {
//      context.client.logMessage(new MessageParams(MessageType.Warning, s"expr ${expr.getClass.getSimpleName} contains position ${position.getLine}:${position.getCharacter}"))
      val tree = context.tree
      val list = tree(expr).children.map(_.node).flatMap(search)
      list :+ expr
    }
  }

  def searchStmt(stmt: Stmt): List[ASTNode] = {
    if(!stmt.containsPosition(position)) {
        return List()
    }
    val tree = context.tree
    val list = tree(stmt).children.map(_.node).flatMap(search)
    list :+ stmt
  }

  def searchTyInfo(info: TyInfo): List[ASTNode] = {
    if(!info.containsPosition(position)) {
      return List()
    }
    List(info)
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
    case expr: Expr => PositionRange.getExprPositionRange(expr)
    case ident: Ident => PositionRange.getIdentPositionRange(ident)
    case methodDecl: MethodDecl => PositionRange.getMethodDeclPositionRange(methodDecl)
//    case modules: Modules => p.visit(modules)
//    case param: Param => p.visit(param)
//    case params: Params => params.params.foreach(p.visit)
//    case rcModule: RcModule => p.visit(rcModule)
    case stmt: Stmt => PositionRange.getStmtPositionRange(stmt)
//    case info: TyInfo => p.visit(info)
//    case item: Item
    case _ => new Range(p.getPosition, p.getPosition)


  def containsPosition(position: Position) = p.getPositionRange.contains(position)
}

object PositionRange {
  def getMethodPositionRange(method: Method) = {
    val begin = addCharacter(method.name.getPosition, -("def ".length))
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

  def addCharacter(position: Position, offset: Int) = {
    new Position(position.getLine, position.getCharacter + offset)
  }

  def addCharacter(position: Position, str: String) = {
    new Position(position.getLine, position.getCharacter + str.length)
  }

  def getListRange(list: List[ASTNode], offset: Int = 0) = {
    if(list.isEmpty) {
      new Range(new Position(0, 0), new Position(0, 0))
    } else {
      val begin = list.head.getPosition
      val end = addCharacter(list.last.getPositionRange.getEnd, offset)
      new Range(begin, end)
    }
  }

  def listOperate(list: List[ASTNode], f: (List[ASTNode] => Position)): Position = {
    if(list.isEmpty) {
      new Position(0, 0)
    } else {
      f(list)
    }
  }

  def getExprPositionRange(expr: Expr) = {
    expr match
      case Expr.Number(v) => new Range(expr.getPosition, addCharacter(expr.getPosition, v.toString))
      case Expr.Identifier(ident) => new Range(expr.getPosition, addCharacter(expr.getPosition, ident.str))
      case Expr.Bool(b) => new Range(expr.getPosition, addCharacter(expr.getPosition, b.toString))
      case Expr.Binary(op, lhs, rhs) => new Range(new Position(op.pos.line, op.pos.column), rhs.getPositionRange.getEnd)
      case Expr.Str(str) => new Range(expr.getPosition, addCharacter(expr.getPosition, str.length + 1))
      case Expr.If(cond, true_branch, false_branch) => ???
      case Expr.Lambda(args, block) => ???
      case Expr.Call(target, args) => getCallRange(target, args)
      case Expr.MethodCall(obj, target, args) => getCallRange(obj, args)
      case Expr.Block(stmts) => getListRange(stmts)
      case Expr.Return(expr) => new Range(addCharacter(expr.getPosition, "return".length + 1), expr.getPositionRange.getEnd)
      case Expr.Field(expr, ident) => new Range(expr.getPosition, getIdentPositionRange(ident).getEnd)
      case Self => new Range(expr.getPosition, addCharacter(expr.getPosition, "self"))
      case Expr.Symbol(ident) => new Range(expr.getPosition, addCharacter(expr.getPosition, ident.str))
      case Expr.Index(expr, i) => ???
      case Expr.Array(len, initValues) => ???

  }

  private def getCallRange(target: ASTNode, args: List[Expr]) = {
    new Range(target.getPosition, listOperate(args, args => addCharacter(args.last.getPositionRange.getEnd, 1)))
  }

  def getStmtPositionRange(stmt: Stmt) = {
    stmt match
      case Stmt.Local(name, tyInfo, value) => new Range(name.getPosition, value.getPositionRange.getEnd)
      case Stmt.Expr(expr) => getExprPositionRange(expr)
      case Stmt.While(cond, body) => new Range(cond.getPosition, body.getPositionRange.getEnd)
      case Stmt.For(init, cond, incr, body) => new Range(init.getPosition, body.getPositionRange.getEnd)
      case Stmt.Assign(name, value) => new Range(name.getPosition, value.getPositionRange.getEnd)
      case Stmt.Break() => new Range(stmt.getPosition, addCharacter(stmt.getPosition, "break"))
      case Stmt.Continue() => new Range(stmt.getPosition, addCharacter(stmt.getPosition, "continue"))
  }

  def getIdentPositionRange(id: Ident) = {
    new Range(id.getPosition, addCharacter(id.getPosition, id.str))
  }

  def getMethodDeclPositionRange(methodDecl: MethodDecl) = {
    val begin = methodDecl.name.getPosition
    val end = methodDecl.outType.getPosition
    new Range(begin, end)
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
