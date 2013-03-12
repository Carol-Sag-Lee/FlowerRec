LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
include ../includeOpenCV.mk
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
#try to load OpenCV.mk from default install location
include $(TOOLCHAIN_PREBUILT_ROOT)/user/share/OpenCV/OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif
LOCAL_MODULE    := GrabCut
LOCAL_SRC_FILES := GrabCut.cpp
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)