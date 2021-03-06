// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: tensorflow/core/framework/summary.proto

package org.tensorflow.framework;

public interface SummaryMetadataOrBuilder extends
    // @@protoc_insertion_point(interface_extends:tensorflow.SummaryMetadata)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * Data that associates a summary with a certain plugin.
   * </pre>
   *
   * <code>.tensorflow.SummaryMetadata.PluginData plugin_data = 1;</code>
   */
  boolean hasPluginData();
  /**
   * <pre>
   * Data that associates a summary with a certain plugin.
   * </pre>
   *
   * <code>.tensorflow.SummaryMetadata.PluginData plugin_data = 1;</code>
   */
  SummaryMetadata.PluginData getPluginData();
  /**
   * <pre>
   * Data that associates a summary with a certain plugin.
   * </pre>
   *
   * <code>.tensorflow.SummaryMetadata.PluginData plugin_data = 1;</code>
   */
  SummaryMetadata.PluginDataOrBuilder getPluginDataOrBuilder();

  /**
   * <pre>
   * Display opName for viewing in TensorBoard.
   * </pre>
   *
   * <code>string display_name = 2;</code>
   */
  String getDisplayName();
  /**
   * <pre>
   * Display opName for viewing in TensorBoard.
   * </pre>
   *
   * <code>string display_name = 2;</code>
   */
  com.google.protobuf.ByteString
      getDisplayNameBytes();

  /**
   * <pre>
   * Longform readable description of the summary sequence. Markdown supported.
   * </pre>
   *
   * <code>string summary_description = 3;</code>
   */
  String getSummaryDescription();
  /**
   * <pre>
   * Longform readable description of the summary sequence. Markdown supported.
   * </pre>
   *
   * <code>string summary_description = 3;</code>
   */
  com.google.protobuf.ByteString
      getSummaryDescriptionBytes();
}
