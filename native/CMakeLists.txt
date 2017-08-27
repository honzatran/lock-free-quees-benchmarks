cmake_minimum_required(VERSION 3.0.2)

list(APPEND CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/pcm-wrapper/cmake)

find_package(JNI REQUIRED)
find_package(PCMWrapper REQUIRED)

add_library(JavaPCM SHARED src/cz_uk_mff_peva_profilers_PmcHandle.cpp)
target_compile_options(JavaPCM PUBLIC "-std=c++11")
target_link_libraries(JavaPCM ${PCM_WRAPPER_LIBRARIES})

message(${JNI_INCLUDE_DIRS})
message(${PCM_WRAPPER_INCLUDE_DIRS})
include_directories(${JNI_INCLUDE_DIRS})
include_directories(${PCM_WRAPPER_INCLUDE_DIRS})

set (HEADERS ./include/cz_uk_mff_peva_profilers_PmcHandle.h)

install(TARGETS JavaPCM DESTINATION ".")
