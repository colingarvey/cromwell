package cromwell.filesystems.blob

import cats.implicits.catsSyntaxValidatedId
import cats.syntax.apply._
import com.typesafe.config.Config
import common.validation.Validation._
import net.ceedubs.ficus.Ficus._

import java.util.UUID

// WSM config is needed for accessing WSM-managed blob containers created in Terra workspaces.
// If the identity executing Cromwell has native access to the blob container, this can be ignored.
final case class WorkspaceManagerConfig(url: WorkspaceManagerURL,
                                        workspaceId: WorkspaceId,
                                        containerResourceId: ContainerResourceId,
                                        overrideWsmAuthToken: Option[String]) // dev-only

final case class BlobFileSystemConfig(endpointURL: EndpointURL,
                                      blobContainerName: BlobContainerName,
                                      subscriptionId: Option[SubscriptionId],
                                      expiryBufferMinutes: Long,
                                      workspaceManagerConfig: Option[WorkspaceManagerConfig])

object BlobFileSystemConfig {

  final val defaultExpiryBufferMinutes = 10L

  def apply(config: Config): BlobFileSystemConfig = {
    val endpointURL = parseString(config, "endpoint").map(EndpointURL)
    val blobContainer = parseString(config, "container").map(BlobContainerName)
    val subscriptionId = parseUUIDOpt(config, "subscription").map(_.map(SubscriptionId))
    val expiryBufferMinutes =
      parseLongOpt(config, "expiry-buffer-minutes")
        .map(_.getOrElse(defaultExpiryBufferMinutes))

    val wsmConfig =
      if (config.hasPath("workspace-manager")) {
        val wsmConf = config.getConfig("workspace-manager")
        val wsmURL = parseString(wsmConf, "url").map(WorkspaceManagerURL)
        val workspaceId = parseUUID(wsmConf, "workspace-id").map(WorkspaceId)
        val containerResourceId = parseUUID(wsmConf, "container-resource-id").map(ContainerResourceId)
        val overrideWsmAuthToken = parseStringOpt(wsmConf, "b2cToken")

        (wsmURL, workspaceId, containerResourceId, overrideWsmAuthToken)
          .mapN(WorkspaceManagerConfig)
          .map(Option(_))
      }
      else None.validNel

    (endpointURL, blobContainer, subscriptionId, expiryBufferMinutes, wsmConfig)
      .mapN(BlobFileSystemConfig.apply)
      .unsafe("Couldn't parse blob filesystem config")
  }

  private def parseString(config: Config, path: String) =
    validate[String] { config.as[String](path) }

  private def parseStringOpt(config: Config, path: String) =
    validate[Option[String]] { config.as[Option[String]](path) }

  private def parseUUID(config: Config, path: String) =
    validate[UUID] { UUID.fromString(config.as[String](path)) }

  private def parseUUIDOpt(config: Config, path: String) =
    validate[Option[UUID]] { config.as[Option[String]](path).map(UUID.fromString) }

  private def parseLongOpt(config: Config, path: String) =
    validate[Option[Long]] { config.as[Option[Long]](path) }
}

// Our filesystem setup magic can't use BlobFileSystemConfig.apply directly, so we need this
// wrapper class.
class BlobFileSystemConfigWrapper(val config: BlobFileSystemConfig) {
  def this(config: Config) = this(BlobFileSystemConfig(config))
}
