package io.github.abudanov.springframework.ai.yandex;

public class EmbeddingUtils extends org.springframework.ai.model.EmbeddingUtils {

	public static final float[] EMPTY_FLOAT_ARRAY = new float[0];

	public static float[] doubleToFloatPrimitive(double[] doubles) {
		float[] floats = new float[doubles.length];
		for (int i = 0; i < doubles.length; i++) {
			floats[i] = (float) doubles[i];
		}
		return floats;
	}

}
