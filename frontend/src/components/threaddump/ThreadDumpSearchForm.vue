<!--
    Copyright (c) 2023 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
    <el-container>
        <el-row :gutter="20" style="width: 100%;">
            <el-col :span="24">
                <el-card class="mt-3" bg-variant="dark" text-variant="white" :header="$t('jifa.threadDumpSearch.searchTitle')" >
                    <el-form :inline="false" :model="search" ref="formRef"  label-width="auto" size="small" :rules="rules">
                        <el-row :gutter="20">
                            <el-col :span="8">
                                <el-form-item :label="$t('jifa.threadDumpSearch.searchInput')" prop="term" >
                                    <el-input clearable size="large" v-model="search.term" @keydown.enter.native.prevent="e => submitSearchForm()" :placeholder="$t('jifa.threadDumpSearch.searchTitle')" prefix-icon="el-icon-search" />
                                </el-form-item>
                            </el-col>
                            <el-col :span="6">
                                <el-button type="success" icon="el-icon-search"
                                    @click="submitSearchForm()">{{ $t('jifa.threadDumpSearch.searchTitle') }}</el-button>
                                <el-checkbox-button style="margin-left: 0px; " border icon="el-icon-edit"
                                    v-model="search.advancedVisible">{{ $t('jifa.threadDumpSearch.advancedToggle') }}</el-checkbox-button>
                            </el-col>
                        </el-row>
                        <el-row :gutter="20" style="width: 100%;" v-if="search.advancedVisible">
                            <el-col :span="6">
                                <el-divider content-position="left">{{ $t('jifa.threadDumpSearch.searchFields') }}</el-divider>
                                    <el-form-item :label="$t('jifa.threadDumpSearch.searchFieldName')">
                                        <el-switch v-model="search.searchName" />
                                    </el-form-item>
                                    <el-form-item :label="$t('jifa.threadDumpSearch.searchFieldState')">
                                        <el-switch v-model="search.searchState" />
                                    </el-form-item>
                                    <el-form-item :label="$t('jifa.threadDumpSearch.searchFieldStack')">
                                        <el-switch v-model="search.searchStack" />
                                    </el-form-item>
                            </el-col>
                            <el-col :span="6">
                                <el-divider content-position="left">{{ $t('jifa.threadDumpSearch.searchOptions') }}</el-divider>
                                <el-form-item :label="$t('jifa.threadDumpSearch.searchOptionRegex')" >
                                    <el-switch v-model="search.regex" />
                                </el-form-item>
                                <el-form-item :label="$t('jifa.threadDumpSearch.searchOptionMatchCase')">
                                    <el-switch v-model="search.matchCase" />
                                </el-form-item>
                                <el-form-item :label="$t('jifa.threadDumpSearch.searchOptionThreadStates')">
                                    <el-select v-model="search.allowedJavaStates" multiple :placeholder="$t('jifa.threadDumpSearch.searchOptionThreadStatesPlaceholder')">
                                        <el-option v-for="item in threadStateOptions" :key="item" :label="item" :value="item"></el-option>
                                    </el-select>
                                </el-form-item>
                            </el-col>
                            <el-col>
                            </el-col>
                        </el-row>
                    </el-form>
                </el-card>
            </el-col>
        </el-row>
    </el-container>
</template>

<script>


export default {
    emits: ['submit'],
    components: {
    },
    data() {
        return {
            search: {
                advancedVisible: null,
                term: null,
                searchState: null,
                searchStack: null,
                searchName: null,
                regex: null,
                matchCase: null,
                allowedJavaStates: null,
                //file: null,
            },
            rules: {
                term: [
                    { required: true, message: 'Please enter a search term', trigger: 'blur' },
                    { validator: (rule, value, callback) => { this.validateSearch(rule, value, callback)}, trigger: 'blur' }]
            },
            validateSearch(rule, value, callback) {
                if(this.search.regex) {
                    try {
                        new RegExp(this.search.term);
                    } catch(e) {
                        callback(new Error(e));
                        return
                    }
                }
                callback()
            },
            threadStateOptions: ['RUNNABLE','SLEEPING','IN_OBJECT_WAIT','IN_OBJECT_WAIT_TIMED','PARKED','PARKED_TIMED','BLOCKED_ON_MONITOR_ENTER','TERMINATED']
        }
    },

    methods: {
        
        submitSearchForm() {
            let form = this.$refs['formRef']
            form.validate((valid) => {
                if (valid) {
                    this.$emit('submit', this.search)
                } else {
                    return false
                }
            })
        },
        //parses a string value into a boolean, returns the default if value is empty or undefined
        toBoolean(value, defaultValue) {
            if(value === 'true') {
                return true;
            }
            if(value === 'false') {
                return false;
            }
            return defaultValue
        }
    },
    mounted() {
        let query = this.$route.query
        let term = query.term
        if(Array.isArray(term)) {
            term = term.join(" ")
        }
        this.search.term = term
        this.search.advancedVisible = this.toBoolean(query.advancedVisible, false)
        this.search.searchState = this.toBoolean(query.searchState, true)
        this.search.searchStack = this.toBoolean(query.searchStack, true)
        this.search.searchName = this.toBoolean(query.searchName, true)
        this.search.regex = this.toBoolean(query.regex, false)
        this.search.matchCase = this.toBoolean(query.matchCase, false)
        if(query.allowedJavaStates) {
            this.search.allowedJavaStates = query.allowedJavaStates
        }
    },

}

</script>