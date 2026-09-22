cmake_minimum_required(VERSION 3.22.1)
project(redhawk_jni CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(PREBUILT ${CMAKE_CURRENT_SOURCE_DIR}/prebuilt/${ANDROID_ABI})

# Header yolları
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/include/llama
    ${CMAKE_CURRENT_SOURCE_DIR}/include/ggml
)

# Statik llama.cpp kütüphaneleri
add_library(llama        STATIC IMPORTED)
add_library(ggml         STATIC IMPORTED)
add_library(ggml-base    STATIC IMPORTED)
add_library(ggml-cpu     STATIC IMPORTED)

set_target_properties(llama     PROPERTIES IMPORTED_LOCATION ${PREBUILT}/libllama.a)
set_target_properties(ggml      PROPERTIES IMPORTED_LOCATION ${PREBUILT}/libggml.a)
set_target_properties(ggml-base PROPERTIES IMPORTED_LOCATION ${PREBUILT}/libggml-base.a)
set_target_properties(ggml-cpu  PROPERTIES IMPORTED_LOCATION ${PREBUILT}/libggml-cpu.a)

# Bizim JNI .so'muz
add_library(redhawk_jni SHARED redhawk_jni.cpp)

target_link_libraries(redhawk_jni
    llama
    ggml
    ggml-cpu
    ggml-base
    android
    log
    m
    z
)
