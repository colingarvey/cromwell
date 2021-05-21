package cromwell.backend.google.pipelines.v2alpha1.api

import com.google.api.services.genomics.v2alpha1.model.{Action, Mount}
import cromwell.backend.google.pipelines.common.action.ActionLabels._
import cromwell.backend.google.pipelines.common.PipelinesApiConfigurationAttributes.GcsTransferConfiguration
import cromwell.backend.google.pipelines.common.api.PipelinesApiRequestFactory.CreatePipelineParameters
import cromwell.backend.google.pipelines.common.PipelinesApiJobPaths._
import cromwell.backend.google.pipelines.v2alpha1.api.ActionBuilder.cloudSdkShellAction
import cromwell.backend.google.pipelines.v2alpha1.api.ActionCommands.localizeFile

trait EgressCheckAction {
  def egressCheckAction(createPipelineParameters: CreatePipelineParameters, mounts: List[Mount])
                       (implicit gcsTransferConfiguration: GcsTransferConfiguration): List[Action] = {
    val egressCheckActionLabel = Map(Key.Tag -> Value.EgressCheckAction)

    val gcsTransferLibraryContainerPath = createPipelineParameters.commandScriptContainerPath.sibling(GcsTransferLibraryName)
    val localizeGcsTransferLibrary = cloudSdkShellAction(localizeFile(
      cloudPath = createPipelineParameters.cloudCallRoot / GcsTransferLibraryName,
      containerPath = gcsTransferLibraryContainerPath))(mounts = mounts, labels = egressCheckActionLabel)

    val gcsEgressCheckContainerPath = createPipelineParameters.commandScriptContainerPath.sibling(GcsEgressCheckScriptName)
    val localizeGcsEgressCheckScript = cloudSdkShellAction(localizeFile(
      cloudPath = createPipelineParameters.cloudCallRoot / GcsEgressCheckScriptName,
      containerPath = gcsEgressCheckContainerPath))(mounts = mounts, labels = egressCheckActionLabel)

    val runGcsEgressCheckScript = cloudSdkShellAction(
      s"/bin/bash $gcsEgressCheckContainerPath")(mounts = mounts, labels = egressCheckActionLabel)

    val egressChecks = List(localizeGcsTransferLibrary, localizeGcsEgressCheckScript, runGcsEgressCheckScript)

    ActionBuilder.annotateTimestampedActions("egress check", Value.EgressCheckAction)(egressChecks)
  }
}