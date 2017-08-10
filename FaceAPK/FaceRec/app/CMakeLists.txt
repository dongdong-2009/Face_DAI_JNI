# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

#经常配合set命令使用的CMake变量，使用set(variable value)进行设置
#如：设置工程路径
#set(pathToProject E:/AndroidStudioProjects/FaceRecDemo2)
#OpenCV-android-sdk路径
#set(pathToOpenCv E:/AndroidStudioProjects/FaceRecDemo2/app)

# Sets the minimum version of CMake required to build the native library.
#CMAKE版本
cmake_minimum_required(VERSION 3.4.1)

#输出详细的编译和链接信息
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
#set(CMAKE_CXX_COMPILER "g++") c++编译器
#set(CMAKE_CXX_FLAGS "-Wall") c++编译器参数

#set命令还可以设置自定义变量，比如
#set(MY_GREETINGS "hello world")

#${PROJECT_SOURCE_DIR}表示app目录，
#${CMAKE_CURRENT_SOURCE_DIR}表示CMAKELists.txt所在目录，此处等于APP目录
#设置lib库的生成位置
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

#set(OpenCV_DIR include)
#REQUIRED表示如果没有找到，cmake会停止处理，并报告一个错误.
#find_package(OpenCV REQUIRED)

#配置加载native依赖头文件
#include_directories(${pathToOpenCv}/include)
INCLUDE_DIRECTORIES(${CMAKE_CURRENT_SOURCE_DIR}/include )

#指定用到的系统库或者NDK库或者第三方库的搜索路径，可选。
#LINK_DIRECTORIES(/usr/local/lib)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp
             src/main/cpp/DetectionBasedTracker_jni.cpp)

add_library( lib_opencv SHARED IMPORTED )
add_library( lib_face_rec SHARED IMPORTED )

#设置要连接的so的相对路径，${ANDROID_ABI}表示so文件的ABI类型的路径,这一步引入了动态加入编译的so
#set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)
#set_target_properties(lib_face_rec PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libface_rec.so)

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)
set_target_properties(lib_face_rec PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libface_rec.so)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       lib_opencv
                       lib_face_rec )