import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props,ActorLogging}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source

class FileOperation extends Actor {

  override def receive: Receive = {
    case "checkFilesOrFoldersList" =>
      val files = checkFile(FileHandling.filesOrFoldersList)

      sender() ! files
    case "readFile" =>
      val contents = readFile(FileHandling.filesList)
      sender() ! contents
    case "readError" =>
      val error = readError(FileHandling.content)
      sender() ! error
    case "readWarnings" =>
      val warnings = readWarnings(FileHandling.content)
      sender() ! warnings
    case "readInfo" =>
      val info = readInfo(FileHandling.content)
      sender() ! info
    case "averageWarnings" =>
      val warnings = averageWarnings(FileHandling.warnings)
      sender() ! warnings
    case _ => sender() ! "invalid"


  }

  def checkFile(filesOrFolder: List[File]): List[File] = {
    filesOrFolder.filter(_.isFile)
  }


  def readFile(files: List[File]): List[(String, List[String])] = {
    files.map((file => (file.getName, Source.fromFile(file).getLines().toList)))


  }

  def readError(filesContent: List[(String, List[String])]): List[(String, Int)] = {
    filesContent.map(fileNameOrContent => (fileNameOrContent._1, fileNameOrContent._2.count(_.contains("[ERROR]"))))


  }

  def readWarnings(filesContent: List[(String, List[String])]): List[(String, Int)] = {

    filesContent.map(fileNameOrContent => (fileNameOrContent._1, fileNameOrContent._2.count(_.contains("[WARN]"))))

  }

  def readInfo(filesContent: List[(String, List[String])]): List[(String, Int)] = {
    filesContent.map(fileNameOrContent => (fileNameOrContent._1, fileNameOrContent._2.count(_.contains("[INFO]"))))

  }

  def averageWarnings(warningsCount: List[(String, Int)]): Int = {
    warningsCount.foldLeft(0) { (sum, warningList) => sum + warningList._2 } / warningsCount.length

  }

}

object FileHandling extends App {
  val system = ActorSystem("LogFilesActorSystem")
  val ref = system.actorOf(Props[FileOperation], "FileHandling")
  val pathObj = new File("/home/knoldus/Downloads/logfiles")
  val filesOrFoldersList = pathObj.listFiles().toList
  implicit val timeout = Timeout(5. seconds)
  val futureFilesList = (ref ? "checkFilesOrFoldersList").mapTo[List[File]]
  val filesList = Await.result(futureFilesList, 15.seconds)
  val futureContent = (ref ? "readFile").mapTo[List[(String, List[String])]]
  val content = Await.result(futureContent, 5.seconds)
  val futureError = (ref ? "readError").mapTo[List[(String, Int)]]
  val error = (Await.result(futureError, 5.seconds))
  val futureWarnings = (ref ? "readWarnings").mapTo[List[(String, Int)]]
  val warnings = (Await.result(futureWarnings, 5.seconds))
  val futureInfo = (ref ? "readInfo").mapTo[List[(String, Int)]]
  val info = (Await.result(futureInfo, 5.seconds))
  val futureaverageWarnings = (ref ? "averageWarnings").mapTo[Int]
  val averagewarnings = (Await.result(futureaverageWarnings, 5.seconds))

  println(averagewarnings)


}
