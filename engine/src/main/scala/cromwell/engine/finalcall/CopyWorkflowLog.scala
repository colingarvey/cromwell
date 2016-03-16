package cromwell.engine.finalcall

import cromwell.engine._
import cromwell.webservice.WorkflowMetadataResponse

import scala.concurrent.ExecutionContext

object CopyWorkflowLogCall extends FinalCallCompanion[CopyWorkflowLogCall] {
  override val finalCallName = "copy_workflow_log"

  override def createCall(workflow: WorkflowDescriptor) = CopyWorkflowLogCall(workflow)
}

/**
  * Final call implementation that copies the workflow log to a specified destination.
  */
case class CopyWorkflowLogCall(override val workflowDescriptor: WorkflowDescriptor) extends FinalCall {
  override val companion = CopyWorkflowLogCall
  override val handle = CopyWorkflowLogHandle

  override def execute(workflowMetadataResponse: WorkflowMetadataResponse)(implicit ec: ExecutionContext) = {
    workflowDescriptor.copyWorkflowLog()
  }
}

case object CopyWorkflowLogHandle extends FinalCallHandle
