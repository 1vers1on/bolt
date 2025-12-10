package net.ellie.bolt.decoder;

sealed interface DecoderPipelineData permits TextData, BytesData {}
