# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
BEGIN {
    RS = " "
    modules[n++] = "core"
    pmodules[pn++] = "core"
} 
{
    modules[n] = $1;
    pmodules[pn] = $1;
    gsub("\n","",modules[n]);
    gsub("\n","",pmodules[pn]);
    ++n;
    ++pn;
} 
END {
    print "/*"
    print " * modules.c --- automatically generated by Apache"
    print " * configuration script.  DO NOT HAND EDIT!!!!!"
    print " */"
    print ""
    print "#include \"ap_config.h\""
    print "#include \"httpd.h\""
    print "#include \"http_config.h\""
    print ""
    for (i = 0; i < pn; ++i) {
        printf ("extern module %s_module;\n", pmodules[i])
    }
    print ""
    print "/*"
    print " *  Modules which implicitly form the"
    print " *  list of activated modules on startup,"
    print " *  i.e. these are the modules which are"
    print " *  initially linked into the Apache processing"
    print " *  [extendable under run-time via AddModule]"
    print " */"
    print "module *ap_prelinked_modules[] = {"
    for (i = 0 ; i < n; ++i) {
        printf "  &%s_module,\n", modules[i]
    }
    print "  NULL"
    print "};"
    print ""
    print "/*"
    print " *  We need the symbols as strings for <IfModule> containers"
    print " */"
    print ""
    print "ap_module_symbol_t ap_prelinked_module_symbols[] = {"
    for (i = 0; i < n; ++i) {
        printf ("  {\"%s_module\", &%s_module},\n", modules[i], modules[i])
    }
    print "  {NULL, NULL}"
    print "};"
    print ""
    print "/*"
    print " *  Modules which initially form the"
    print " *  list of available modules on startup,"
    print " *  i.e. these are the modules which are"
    print " *  initially loaded into the Apache process"
    print " *  [extendable under run-time via LoadModule]"
    print " */"
    print "module *ap_preloaded_modules[] = {"
    for (i = 0; i < pn; ++i) {
        printf "  &%s_module,\n", pmodules[i]
    }
    print "  NULL"
    print "};"
    print ""
}
