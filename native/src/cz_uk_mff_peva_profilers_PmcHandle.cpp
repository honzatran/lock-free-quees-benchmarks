#include "../include/cz_uk_mff_peva_profilers_PmcHandle.h" 
#include "../include/cz_uk_mff_peva_profilers_PmcCoreCounterHandle.h"

#include <iostream>
#include <fstream>
#include <unistd.h>

#include <pcm/utils.h>

#include <hw_counter_json_reader.hpp>
#include <pcm_context.hpp>

using namespace std;
using namespace PcmWrapper;

std::unique_ptr<PcmContext> g_context;


#ifdef __linux__
using CoreHandle = RDPMCCountersHandle;
#else
using CoreHandle = CoreCountersHandle;
#endif

std::vector<std::unique_ptr<CounterHandleRecorder<CoreHandle>>> g_coreHandles;
std::vector<std::unique_ptr<CounterHandleRecorder<SystemCountersHandle>>> g_systemHandles;

#if __cplusplus < 201402L

template <typename T, typename... ARGS>
std::unique_ptr<T> 
make_unique(ARGS&&... args)
{
    return std::unique_ptr<T>(new T(std::forward<ARGS>(args)...));
}


#endif

std::string toCppString(JNIEnv *env, jstring javaString) 
{
    const char *charPtr = env->GetStringUTFChars(javaString, nullptr); 
    std::string s = charPtr;
    env->ReleaseStringUTFChars(javaString, charPtr);

    return s;
}

template <PcmWrapper::CounterRegister REGISTER>
void setEvent(JNIEnv *env, jstring javaCounterName)
{
    if (g_context) {
        std::string counterName = toCppString(env, javaCounterName);
        g_context->setCounter<REGISTER>(counterName);
    }
}


jboolean
Java_cz_uk_mff_peva_profilers_PmcHandle_initialize(JNIEnv *env, jobject object, jstring j_filename) {

    const char *content = env->GetStringUTFChars(j_filename, nullptr); 
    std::string s = content;

    env->ReleaseStringUTFChars(j_filename, content);

    cout << "filename" << " " << s << endl;

    PcmWrapper::HwCounterJsonReader reader;

    reader.loadFromDirectory(s);

    g_context = make_unique<PcmContext>();
    g_context->init(reader);

    return false;
}

void
Java_cz_uk_mff_peva_profilers_PmcHandle_onBegin(JNIEnv *, jobject) {
    cout << "START" << endl;
}

/*
 * Class:     cz_uk_mff_peva_profilers_PmcHandle
 * Method:    onEnd
 * Signature: ()V
 */
void 
Java_cz_uk_mff_peva_profilers_PmcHandle_onEnd(JNIEnv *, jobject) {
    cout << "END" << endl;
}

/*
 * Class:     cz_uk_mff_peva_profilers_PmcHandle
 * Method:    destroy
 * Signature: ()V
 */
void JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_destroy(JNIEnv *, jobject) {
    if (g_context) {
        g_context = nullptr;
    }
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_setFirstCounter(JNIEnv *env,
                                                        jobject object,
                                                        jstring counterName) {
    setEvent<PcmWrapper::ONE>(env, counterName);
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_setSecondCounter(JNIEnv *env,
                                                         jobject object,
                                                         jstring counterName) {
    setEvent<PcmWrapper::TWO>(env, counterName);
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_setThirdCounter(JNIEnv *env,
                                                        jobject object,
                                                        jstring counterName) {
    setEvent<PcmWrapper::THREE>(env, counterName);
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_setForthCounter(JNIEnv *env,
                                                        jobject object,
                                                        jstring counterName) {
    setEvent<PcmWrapper::FOUR>(env, counterName);
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_startMeasuring(JNIEnv *env,
                                                       jobject object) {
    if (g_context) {
        g_context->startMonitoring();
    }
}

jobject JNICALL
Java_cz_uk_mff_peva_profilers_PmcHandle_getCoreProfiler(JNIEnv *env,
                                                        jobject object,
                                                        jint cpu,
                                                        jint operation) {
    if (!g_context) {
        return nullptr;
    } 

#ifdef __linux__
    auto handle = CoreHandle();
#else
    auto handle = g_context->getCoreHandle(cpu);
#endif

    auto mixin = CounterHandleRecorder<decltype(handle)>(
        operation, PcmWrapper::FOUR, std::move(handle));

    g_coreHandles.push_back(
        make_unique<decltype(mixin)>(std::move(mixin)));

    jclass cls = env->FindClass("cz/uk/mff/peva/profilers/PmcCoreCounterHandle");

    jmethodID constructor = env->GetMethodID(cls, "<init>", "(I)V");

    return env->NewObject(cls, constructor, g_coreHandles.size() - 1);
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcCoreCounterHandle_onBegin(JNIEnv *env,
                                                           jobject object,
                                                           jint index) {
    auto const& p = g_coreHandles[index];
    p->onStart();
}

/*
 * Class:     cz_uk_mff_peva_profilers_PmcCoreCounterHandle
 * Method:    onEnd
 * Signature: (I)V
 */
void JNICALL
Java_cz_uk_mff_peva_profilers_PmcCoreCounterHandle_onEnd(JNIEnv *env,
                                                         jobject object,
                                                         jint index) {
    auto const& p = g_coreHandles[index];
    p->onEnd();
}

/*
 * Class:     cz_uk_mff_peva_profilers_PmcCoreCounterHandle
 * Method:    reset
 * Signature: (I)V
 */
void JNICALL
Java_cz_uk_mff_peva_profilers_PmcCoreCounterHandle_reset(JNIEnv *env,
                                                         jobject object,
                                                         jint index) {
    auto const& p = g_coreHandles[index];
    p->reset();
}

void JNICALL
Java_cz_uk_mff_peva_profilers_PmcCoreCounterHandle_recordResult(JNIEnv * env,
                                                                jobject object,
                                                                jint index,
                                                                jstring filepath)
{
    std::string outputFilename = toCppString(env, filepath);
    std::ofstream oss(outputFilename);

    oss << g_context->getEventHeader() << endl;
    auto const& p = g_coreHandles[index];
    oss << *p;
}



