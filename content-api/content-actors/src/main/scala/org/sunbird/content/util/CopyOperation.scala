package org.sunbird.content.util

import org.apache.commons.lang3.StringUtils
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.dac.model.{Node, Relation}
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.collections.MapUtils
import org.sunbird.common.Platform
import org.sunbird.graph.common.Identifier
import java.util
import scala.collection.JavaConverters._

object CopyOperation {

  def validateCopyContentRequest(existingNode: Node, requestMap: util.Map[String, AnyRef], mode: String): Node = {
    if (null == requestMap)
      throw new ClientException("ERR_INVALID_REQUEST", "Please provide valid request")
    val keys:List[String] = List("createdBy", "createdFor", "organisation", "framework")
    validateOrThrowExceptionForEmptyKeys(requestMap, "Content", keys)
    var notCoppiedContent:util.List[String] = null
    if (Platform.config.hasPath("learning.content.type.not.copied.list"))
      notCoppiedContent = Platform.config.getStringList("learning.content.type.not.copied.list")
    if (!CollectionUtils.isEmpty(notCoppiedContent) && notCoppiedContent.contains(existingNode.getMetadata.get("contentType").asInstanceOf[String]))
      throw new ClientException("CONTENTTYPE_ASSET_CAN_NOT_COPY", "ContentType " + existingNode.getMetadata.get("contentType").asInstanceOf[String] + " can not be copied.")
    val status = existingNode.getMetadata.get("status").asInstanceOf[String]
    val invalidStatusList = Platform.config.getStringList("learning.content.copy.invalid_status_list")
    if (invalidStatusList.contains(status))
      throw new ClientException("ERR_INVALID_REQUEST", "Cannot copy content in " + status.toLowerCase + " status")
    existingNode
  }

   def validateOrThrowExceptionForEmptyKeys(requestMap: util.Map[String, AnyRef], prefix: String, keys: List[String]): Boolean = {
    var errMsg = "Please provide valid value for "
    var flag = false
    val notFoundKeys:util.List[String] = null
    for (key <- keys) {
      if (null == requestMap.get(key))
        flag = true
      else if (requestMap.get(key).isInstanceOf[util.Map[String, AnyRef]])
        flag = MapUtils.isEmpty(requestMap.get(key).asInstanceOf[util.Map[String, AnyRef]])
      else if (requestMap.get(key).isInstanceOf[util.List[String]])
        flag = CollectionUtils.isEmpty(requestMap.get(key).asInstanceOf[util.List[String]])
      else
        flag = StringUtils.isEmpty(requestMap.get(key).asInstanceOf[String])
      if (flag) {
        notFoundKeys.add(key)
      }
    }
    if (CollectionUtils.isEmpty(notFoundKeys))
      return true
    else
      errMsg = errMsg + String.join(", ", notFoundKeys) + "."
    throw new ClientException("ERR_INVALID_REQUEST", errMsg.trim.substring(0, errMsg.length - 1))
  }

  def copyNode(existingNode: Node, requestMap: util.Map[String, AnyRef], mode: String): Node = {
    val newId = Identifier.getIdentifier(existingNode.getGraphId, Identifier.getUniqueIdFromTimestamp)
    val copyNode = new Node(newId, existingNode.getNodeType, existingNode.getObjectType)
    val metaData = new util.HashMap[String, AnyRef](existingNode.getMetadata)
    val originData = scala.collection.mutable.Map[String,AnyRef]()
    var originNodeMetadataList:util.List[String] = new java.util.ArrayList[String]()
      if (Platform.config.hasPath("learning.content.copy.origin_data"))
        originNodeMetadataList = Platform.config.getStringList("learning.content.copy.origin_data")
    if (CollectionUtils.isNotEmpty(originNodeMetadataList)) {
      originNodeMetadataList.asScala.foreach(meta => {
        if (metaData.containsKey(meta))
          originData.put(meta, metaData.get(meta))
      })
    }
    var nullPropList:util.List[String] = null
    if (Platform.config.hasPath("learning.content.copy.props_to_remove"))
      nullPropList =  Platform.config.getStringList("learning.content.copy.props_to_remove")
    if (CollectionUtils.isNotEmpty(nullPropList))
      nullPropList.asScala.foreach(prop => metaData.remove(prop))
    copyNode.setMetadata(metaData)
    copyNode.setGraphId(existingNode.getGraphId)
    requestMap.remove("mode")
    copyNode.getMetadata.putAll(requestMap)
    copyNode.getMetadata.put("status", "Draft")
    copyNode.getMetadata.put("origin", existingNode.getIdentifier)
    copyNode.getMetadata.put("identifier", newId)
    if (originData.nonEmpty)
      copyNode.getMetadata.put("originData", originData)
    val existingNodeOutRelations:util.List[Relation] = existingNode.getOutRelations
    val copiedNodeOutRelations:util.List[Relation] = null
    if (!CollectionUtils.isEmpty(existingNodeOutRelations)) {
      for (rel <- existingNodeOutRelations.asScala) {
        if (!("Content", "ContentImage").asInstanceOf[List[String]].contains(rel.getEndNodeObjectType))
          copiedNodeOutRelations.add(new Relation(newId, rel.getRelationType, rel.getEndNodeId))
      }
    }
    copyNode.setOutRelations(copiedNodeOutRelations)
    copyNode
  }

}
