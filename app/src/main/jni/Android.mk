# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := NativeSQLite
LOCAL_SRC_FILES := com_epam_database_NativeSQLiteConnection.cpp

include $(BUILD_STATIC_LIBRARY)

#LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog -lz -lm -landroid -lrt -ldl $(LIB_PATH) -lxtract -latomic -lutils
#LOCAL_LDLIBS := -llog -lz -lm -landroid -lrt -ldl -lxtract -latomic -lutils

#LOCAL_CFLAGS += -DHAVE_SYS_UIO_H
#LOCAL_CPP_FEATURES += exceptions

LOCAL_STATIC_LIBRARIES :=  libutils \
                    	libcutils