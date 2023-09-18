/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
export default {
  jifa: {
    common: {
      confirm: 'Confirm',
      submit: 'Submit',
      operations: 'Operations',
      copy: 'Copy',
      search: 'Search',
      result: 'Result',
      description: 'Description',
      detail: 'Detail',
      noData: 'No Data',
      file: 'File',
      messageBeforeLeave: 'Sure to leave?',
      clickToLoadMore: 'Click to load more data',
      expandedColumnWidth: '+ Width'
    },

    header: {
      login: 'Login',
      logout: 'Logout'
    },

    serviceUnavailable: {
      title: 'Service is unavailable',
      subtitle: 'Please contact the administrator'
    },

    file: {
      name: 'Name',
      type: 'Type',
      size: 'Size',
      uploadedTime: 'Uploaded Time',
      operations: 'Operations',

      all: 'All Types',
      heapDump: 'Heap Dump',
      GCLog: 'GC Log',
      threadDump: 'Thread Dump',

      new: 'New File',

      analyze: 'Analyze'
    },

    form: {
      requiredMessage: "Please enter '{0}'",
      selectionRequiredMessage: "Please select '{0}'",
      invalidLengthMessage: "Length of '{0}' should be {1} to {2}"
    },

    loginForm: {
      login: 'Log in',
      loginTitle: 'Hi, Log in to your Account',
      loginSubtitle: "Don't have an account yet?",
      signup: 'Sign up',
      signupTitle: 'Register a new Account',
      signupSubtitle: 'Already have an account?',
      usernameLabel: 'Username',
      usernamePlaceholder: "Enter your username, e.g. grace{'@'}gmail.com",
      passwordLabel: 'Password',
      passwordPlaceholder: 'Enter your password',
      fullNameLabel: 'Full Name',
      fullNamePlaceholder: 'Enter your name',
      emailLabel: 'Email',
      usedAsUsername: ' (will be used as Username)',
      emailPlaceholder: "Enter your email, e.g. e.g. grace{'@'}gmail.com",
      or: 'or',
      continueWith: 'Continue with {0}',
      invalidEmailMessage: 'Please enter a valid Email'
    },

    fileTransferForm: {
      transferMethod: 'Method',
      type: 'Type',

      upload: 'Upload',

      dragOrClickToUpload: 'Drop file here or click to upload',

      host: 'Host',
      user: 'User',
      authentication: 'Authentication',
      password: 'Password',
      publicKey: 'Public Key',
      path: 'Path',

      publicKeyCopyPrompt: 'Please copy key to ~/.ssh/authorized_keys'
    },

    analysis: {
      log: 'Log',
      success: 'Analysis successful'
    },

    heapDump: {
      option: {
        descOfKeepUnreachableObjects:
          'If this option is enabled, the analyzer will treat the unreachable objects as the unreachable roots, and then analyze the reference relationship in them. Otherwise, no further analysis will be performed on the unreachable objects, but only the statistics of types of the unreachable object, that is Unreachable Objects Histogram.',
        descOfStrictness: "'strictness' indicates the follow-up action when an error occurred",
        descOfStopStrictness: 'Throw an error and stop analyzing the dump',
        descOfWarnStrictness: 'Raise a warning and continue',
        descOfPermissiveStrictness: 'Raise a warning and try to "fix" it'
      },

      tab: {
        overview: 'Overview',
        leakSuspects: 'Leak Suspects',
        dominatorTree: 'Dominator Tree',
        GCRoots: 'GC Roots',
        histogram: 'Histogram',
        threads: 'Threads',
        unreachableObjects: 'Unreachable Objects',
        systemProperties: 'System Properties',
        directByteBuffers: 'Direct Byte Buffers',
        classLoaders: 'Class Loaders',
        duplicateClasses: 'Duplicated Classes',
        query: 'OQL',

        inspector: 'Inspector',
        statics: 'Statics',
        attributes: 'Attributes',
        value: 'Value'
      },

      overview: {
        basicInformation: 'Basic Information',
        usedHeapSize: 'Used Heap Size',
        numberOfClasses: 'Class Count',
        numberOfObjects: 'Object Count',
        numberOfClassLoaders: 'Class Loaders Count',
        numberOfGCRoots: 'GC Root Count',
        creationDate: 'Creation Date',
        identifierSize: 'OS Bit',

        biggestObjectsChartTitle: 'Biggest Objects (by Retained Size)'
      },

      column: {
        className: 'Class Name',
        percentage: 'Percentage',
        objectCount: 'Object Count',
        superClass: 'Super Class',
        classLoader: 'Class Loader',

        key: 'Key',
        value: 'Value'
      },

      field: {
        type: 'Type',
        name: 'Name',
        value: 'Value'
      },

      dynamicTab: {
        objectOutbounds: 'Outgoing References',
        objectInbounds: 'Incoming References',
        classOutbounds: 'Outgoing References',
        classInbounds: 'Incoming References',
        pathToGCRoots: 'Path to GC Roots',
        mergedPathToGCRoots: 'Merged Path to GC Roots'
      },

      contextmenu: {
        referencesByObject: 'References by Object',
        referencesByClass: 'References by Class',
        outbounds: 'Outgoing',
        inbounds: 'Incoming',
        pathToGCRoots: 'Path to GC Roots',
        mergedPathToGCRoots: 'Merged Path to GC Roots'
      },

      placeholder: {
        query: 'Enter {0}. Click the link icon in the selection box for detailed help.'
      }
    }
  }
};
