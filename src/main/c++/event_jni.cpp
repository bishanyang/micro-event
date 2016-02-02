#include "edu_cmu_ml_rtw_micro_event_EventExtractor.h"

#include <jni.h>
#include <stdlib.h>
#include <string.h>

extern "C" {
void set_config_path(const char* new_path);
char* event_parse(const char* input_data);
}

JNIEXPORT jboolean JNICALL Java_edu_cmu_ml_rtw_micro_event_EventExtractor_initialize(
		JNIEnv* env, jobject obj, jstring config_file)
{
	const char* config_filename = env->GetStringUTFChars(config_file, 0);
	if (config_filename == NULL) {
		fprintf(stderr, "(JNI) initialize ERROR: Unable to retrieve Java string 'config_file'.\n");
		return false;
	}
	set_config_path(config_filename);

	env->ReleaseStringUTFChars(config_file, config_filename);

	return true;
}

JNIEXPORT jstring JNICALL Java_edu_cmu_ml_rtw_micro_event_EventExtractor_annotate(
		JNIEnv* env, jobject obj, jstring input_data)
{
	const char* input_str = env->GetStringUTFChars(input_data, 0);
	if (input_str == NULL) {
		fprintf(stderr, "(JNI) annotate ERROR: Unable to retrieve Java string 'sentence'.\n");
		return NULL;
	}

	char* result = event_parse(input_str);
	if (result == NULL) {
		env->ReleaseStringUTFChars(input_data, input_str);
		return NULL;
	}
	jstring result_str = env->NewStringUTF(result);

	env->ReleaseStringUTFChars(input_data, input_str);
	free(result);

	return result_str;
}
