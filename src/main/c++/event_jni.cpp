#include "edu_cmu_ml_rtw_micro_event_EventExtractor.h"

#include <jni.h>
#include <stdlib.h>
#include <string.h>

extern "C" {
char* event_parse(const char* input_data, const char* res, const char* wordvec,
		const char* entity_model, const char* event_model, const char* tree_model,
		const char* subtype_role, const char *argrole);
}

JNIEXPORT jstring JNICALL Java_edu_cmu_ml_rtw_micro_event_EventExtractor_annotate(
		JNIEnv* env, jobject obj, jstring input_data, jstring res, jstring wordvec,
		jstring entity_model, jstring event_model, jstring tree_model,
		jstring subtype_role, jstring argrole)
{
	const char* input_str = env->GetStringUTFChars(input_data, 0);
	if (input_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'input_str'.\n");
		return NULL;
	}

	const char* res_str = env->GetStringUTFChars(res, 0);
	if (res_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'res_str'.\n");
		return NULL;
	}

	const char* wordvec_str = env->GetStringUTFChars(wordvec, 0);
	if (wordvec_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'wordvec_str'.\n");
		return NULL;
	}

	const char* entity_model_str = env->GetStringUTFChars(entity_model, 0);
	if (entity_model_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'entity_model_str'.\n");
		return NULL;
	}

	const char* event_model_str = env->GetStringUTFChars(event_model, 0);
	if (event_model_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'event_model_str'.\n");
		return NULL;
	}

	const char* tree_model_str = env->GetStringUTFChars(tree_model, 0);
	if (tree_model_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'tree_model_str'.\n");
		return NULL;
	}

	const char* subtype_role_str = env->GetStringUTFChars(subtype_role, 0);
	if (subtype_role_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'subtype_role_str'.\n");
		return NULL;
	}

	const char* argrole_str = env->GetStringUTFChars(argrole, 0);
	if (argrole_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'argrole_str'.\n");
		return NULL;
	}

	char* result = event_parse(input_str, res_str, wordvec_str,
			entity_model_str, event_model_str, tree_model_str, subtype_role_str, argrole_str);

	if (result == NULL) {
		env->ReleaseStringUTFChars(input_data, input_str);
		env->ReleaseStringUTFChars(input_data, res_str);
		env->ReleaseStringUTFChars(input_data, wordvec_str);
		env->ReleaseStringUTFChars(input_data, entity_model_str);
		env->ReleaseStringUTFChars(input_data, event_model_str);
		env->ReleaseStringUTFChars(input_data, tree_model_str);
		env->ReleaseStringUTFChars(input_data, subtype_role_str);
		env->ReleaseStringUTFChars(input_data, argrole_str);

		return NULL;
	}

	jstring result_str = env->NewStringUTF(result);

	env->ReleaseStringUTFChars(input_data, input_str);
	env->ReleaseStringUTFChars(input_data, res_str);
	env->ReleaseStringUTFChars(input_data, wordvec_str);
	env->ReleaseStringUTFChars(input_data, entity_model_str);
	env->ReleaseStringUTFChars(input_data, event_model_str);
	env->ReleaseStringUTFChars(input_data, tree_model_str);
	env->ReleaseStringUTFChars(input_data, subtype_role_str);
	env->ReleaseStringUTFChars(input_data, argrole_str);

	free(result);

	return result_str;
}
