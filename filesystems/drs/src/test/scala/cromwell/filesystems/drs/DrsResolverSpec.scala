package cromwell.filesystems.drs

import java.nio.channels.ReadableByteChannel
import java.util.Date

import cats.effect.IO
import cloud.nio.impl.drs.MarthaResponse
import com.google.auth.oauth2.{AccessToken, OAuth2Credentials}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.impl.client.HttpClientBuilder
import org.scalatest.{FlatSpec, Matchers}


class DrsResolverSpec extends FlatSpec with Matchers {

  private val marthaConfig: Config = ConfigFactory.parseString(
    """martha {
      |   url = "http://martha-url"
      |   request.json-template = "{"key": "${holder}"}"
      |}
      |""".stripMargin
  )

  private lazy val fakeAccessToken = new AccessToken("ya29.1234567890qwertyuiopasdfghjklzxcvbnm", new Date())
  private lazy val fakeOAuth2Creds = OAuth2Credentials.create(fakeAccessToken)

  private lazy val httpClientBuilder = HttpClientBuilder.create()

  private def drsReadInterpreter(marthaResponse: MarthaResponse): IO[ReadableByteChannel] = ???

  private val mockFileSystemProvider = new MockDrsCloudNioFileSystemProvider(marthaConfig, fakeOAuth2Creds, httpClientBuilder, drsReadInterpreter)
  private val drsPathBuilder = DrsPathBuilder(mockFileSystemProvider)

  val gcsRelativePath = "mybucket/foo.txt"


  behavior of "DrsResolver"

  it should "find GCS path when its the only one in url array" in {
    val drsPath = drsPathBuilder.build(MockDrsPaths.drsPathResolvingToOneGcsPath).get.asInstanceOf[DrsPath]

    DrsResolver.getContainerRelativePath(drsPath).unsafeRunSync() should be (gcsRelativePath)
  }

  it should "find GCS path when DRS path resolves to multiple urls" in {
    val drsPath = drsPathBuilder.build(MockDrsPaths.drsPathResolvingToMultiplePaths).get.asInstanceOf[DrsPath]

    DrsResolver.getContainerRelativePath(drsPath).unsafeRunSync() should be (gcsRelativePath)
  }

  it should "throw GcsUrlNotFoundException when DRS path doesn't resolve to at least one GCS url" in {
    val drsPath = drsPathBuilder.build(MockDrsPaths.drsPathResolvingToNoGcsPath).get.asInstanceOf[DrsPath]

    the[RuntimeException] thrownBy {
      DrsResolver.getContainerRelativePath(drsPath).unsafeRunSync()
    } should have message s"Error while resolving DRS path: ${drsPath.pathAsString}. Error: UrlNotFoundException: No gs url associated with given DRS path."
  }

  it should "throw Runtime Exception when Martha can't find the given DRS path " in {
    val drsPath = drsPathBuilder.build(MockDrsPaths.drsPathNotExistingInMartha).get.asInstanceOf[DrsPath]

    the[RuntimeException] thrownBy {
      DrsResolver.getContainerRelativePath(drsPath).unsafeRunSync()
    } should have message s"Unexpected response resolving ${drsPath.pathAsString} through Martha url http://martha-url. Error: 502 Bad Gateway."
  }
}
