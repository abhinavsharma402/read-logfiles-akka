import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.io.Source

class CheckFile extends Actor {

  override def receive: Receive = {
    case "checkFilesOrFoldersList" =>
      val files1 = checkFile(FileHandling.filesOrFoldersList)
      sender() ! files1
    case "readFile"=>
      val contents=readFile(FileHandling.filesList)
      sender()!contents
    case "readError"=>
      val error=readError(FileHandling.content)
      sender()!error


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
  val ref = system.actorOf(Props[CheckFile], "FileHandling")
  val pathObj = new File("/home/knoldus/Downloads/logfiles")
  val filesOrFoldersList = pathObj.listFiles().toList
  implicit val timeout = Timeout(30. seconds)
  val futureFilesList = (ref ? "filesOrFoldersList").mapTo[List[File]]
  val filesList = Await.result(futureFilesList, 15.seconds)
  val futureContent=(ref ? "readFile").mapTo[List[(String,List[String])]]
  val content=Await.result(futureContent,5.seconds)
 val futureError= (ref ? "readError").mapTo[List[(String,Int)]]
  val error=(Await.result(futureError,5.seconds))
  for(d<-error){
    println(d)

  }
}
