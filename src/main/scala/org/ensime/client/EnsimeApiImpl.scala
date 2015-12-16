package org.ensime.client

import java.io.File

import akka.actor.ActorRef
import akka.util.Timeout
import org.ensime.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask

class EnsimeApiImpl(connection: ActorRef) extends EnsimeApi {
  implicit val timeout = new Timeout(30.seconds)
  override def connectionInfo(): ConnectionInfo = {
    val future = connection.ask(ConnectionInfoReq)

    Await.result(future, timeout.duration).asInstanceOf[ConnectionInfo]
  }

  override def debugValue(loc: DebugLocation): Option[DebugValue] = ???

  override def debugToString(threadId: DebugThreadId, loc: DebugLocation): Option[String] = ???

  override def debugContinue(threadId: DebugThreadId): Boolean = ???

  override def unloadAll(): Unit = ???

  override def docUriForSymbol(typeFullName: String, memberName: Option[String], signatureString: Option[String]): Option[String] = ???

  override def debugStartVM(commandLine: String): DebugVmStatus = ???

  override def debugSetValue(loc: DebugLocation, newValue: String): Boolean = ???

  override def execRefactor(procId: Int, refactorType: RefactorType): Either[RefactorFailure, RefactorResult] = ???

  override def debugRun(): Boolean = ???

  override def debugStepOut(threadId: DebugThreadId): Boolean = ???

  override def completionsAtPoint(fileInfo: SourceFileInfo, point: Int, maxResults: Int, caseSens: Boolean, reload: Boolean): CompletionInfoList = ???

  override def cancelRefactor(procId: Int): Unit = ???

  override def inspectPackageByPath(path: String): Option[PackageInfo] = ???

  override def debugLocateName(threadId: DebugThreadId, name: String): Option[DebugLocation] = ???

  /**
    * Subscribe to async events from the project, replaying previously seen events if requested.
    * The first subscriber will get all undelivered events (subsequent subscribers do not).
    * @param handler The callback handler for events
    * @return True if caller is first subscriber, False otherwise
    */
  override def subscribeAsync(handler: (EnsimeEvent) => Unit): Boolean = ???

  /**
    * Patch the source with the given changes.
    * @param f The file to patch
    * @param edits The patches to apply to the file.
    */
  override def patchSource(f: File, edits: List[PatchOp]): Unit = ???

  override def debugClearAllBreakpoints(): Unit = ???

  override def typeByNameAtPoint(name: String, f: File, range: OffsetRange): Option[TypeInfo] = ???

  override def debugSetBreakpoint(file: File, line: Int): Unit = ???

  override def debugStep(threadId: DebugThreadId): Boolean = ???

  override def debugAttachVM(hostname: String, port: String): DebugVmStatus = ???

  override def typeByName(name: String): Option[TypeInfo] = ???

  override def debugListBreakpoints(): BreakpointList = ???

  override def formatFile(fileInfo: SourceFileInfo): String = ???

  override def docUriAtPoint(f: File, point: OffsetRange): Option[String] = ???

  override def debugClearBreakpoint(file: File, line: Int): Unit = ???

  override def publicSymbolSearch(names: List[String], maxResults: Int): SymbolSearchResults = ???

  override def formatFiles(filenames: List[File]): Unit = ???

  override def importSuggestions(f: File, point: Int, names: List[String], maxResults: Int): ImportSuggestions = ???

  override def packageMemberCompletion(path: String, prefix: String): List[CompletionInfo] = ???

  /**
    * Return detailed type information about the item at the given file position.
    * @param fileName The source filename
    * @param range The range in the file to inspect.
    * @return Some(TypeInspectInfo) if the range represents a valid type, None otherwise
    */
  override def inspectTypeAtPoint(fileName: File, range: OffsetRange): Option[TypeInspectInfo] = ???

  override def typecheckAll(): Unit = ???

  override def removeFile(f: File): Unit = ???

  override def typecheckFiles(fs: List[File]): Unit = ???

  override def prepareRefactor(procId: Int, refactorDesc: RefactorDesc): Either[RefactorFailure, RefactorEffect] = ???

  /**
    * Request the semantic classes of symbols in the given range. These classes are intended to be used for
    * semantic highlighting.
    * Arguments:
    * f source filename
    * start The character offset of the start of the input range.
    * End  The character offset of the end of the input range.
    * requestedTypes The semantic classes in which we are interested. (@see SourceSymbol)
    * Return:
    * SymbolDesignations The given
    */
  override def symbolDesignations(f: File, start: Int, end: Int,
                                  requestedTypes: List[SourceSymbol]): SymbolDesignations = {
    val msg = SymbolDesignationsReq(Left(f),start,end,requestedTypes)
    val future = connection.ask(msg)
    Await.result(future, timeout.duration).asInstanceOf[SymbolDesignations]
  }

  override def typecheckFile(fileInfo: SourceFileInfo): Unit = {
    val msg = TypecheckFileReq(fileInfo)
    val future = connection.ask(msg)
    Await.result(future, timeout.duration).asInstanceOf[org.ensime.api.RpcResponse]
  }

  /**
    * Lookup detailed type description by fully qualified class name
    * @param typeFQN The fully qualified type name to inspect
    * @return Some(TypeInspectInfo) if typeFQN represents a valid type, None otherwise
    */
  override def inspectTypeByName(typeFQN: String): Option[TypeInspectInfo] = ???

  /**
    * Lookup detailed type description by typeId
    * @param typeId The id of the type to inspect (returned by other calls)
    * @return Some(TypeInspectInfo) if the typeId represents a valid type, None otherwise
    */
  override def inspectTypeById(typeId: Int): Option[TypeInspectInfo] = ???

  /**
    * Lookup a detailed symbol description.
    * @param fullyQualifiedName The fully qualified name of a type, object or package.
    * @param memberName The short name of a member symbol of the qualified symbol.
    * @return signatureString An optional signature to disambiguate overloaded methods.
    */
  override def symbolByName(fullyQualifiedName: String, memberName: Option[String], signatureString: Option[String]): Option[SymbolInfo] = ???

  override def usesOfSymAtPoint(f: File, point: Int): List[ERangePosition] = ???

  override def debugNext(threadId: DebugThreadId): Boolean = ???

  override def expandSelection(filename: File, start: Int, stop: Int): FileRange = ???

  override def typeAtPoint(f: File, range: OffsetRange): Option[TypeInfo] = ???

  override def debugBacktrace(threadId: DebugThreadId, index: Int, count: Int): DebugBacktrace = ???

  override def symbolAtPoint(fileName: File, point: Int): Option[SymbolInfo] = ???

  override def callCompletion(id: Int): Option[CallCompletionInfo] = ???

  override def debugActiveVM(): Boolean = ???

  override def typeById(id: Int): Option[TypeInfo] = ???

  override def debugStopVM(): Boolean = ???

  /**
    * Shutdown the server instance
    * N.b. This will kill the server - should be moved out.
    */
  override def shutdownServer(): Unit = ???
}
