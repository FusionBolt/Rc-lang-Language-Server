import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.services.LanguageClient

import java.util.concurrent.Executors
import scala.util.control.NonFatal
import java.io.*

@main def main() = {
  val systemIn = System.in
  val systemOut = System.out
  val exec = Executors.newCachedThreadPool()
  val printer = new PrintWriter(new FileOutputStream(s"/Users/homura/Code/Rc-lang-Language-Server/pc.stdout.log"))
  val server = new RclangLanguageServer()
  
  try {
    val launcher = new Launcher.Builder[RcLanguageClient]()
      .traceMessages(printer)
      .setExecutorService(exec)
      .setInput(systemIn)
      .setOutput(systemOut)
      .setRemoteInterface(classOf[RcLanguageClient])
      .setLocalService(server)
      .create()
    val clientProxy = launcher.getRemoteProxy
    server.connect(clientProxy)
    launcher.startListening().get()
  } catch {
    case NonFatal(e) =>
      e.printStackTrace(systemOut)
      sys.exit(1)
  } finally {
    server.exit()
  }
}