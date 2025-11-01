package com.example.muritin

class ruless {
/*

{
  "rules": {
    "users": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
        "phone": {
          ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
        },
        "age": {
          ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
        },
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
        },
        "ownerId": {
          ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
        }
      }
    },
    "buses": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null",
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
        ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'fares', 'createdAt'])",
        "fares": {
          "$stop": {
            ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
            "$dest": {
              ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
            }
          }
        }
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
        ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
      }
    },
    "schedules": {
      ".indexOn": ["busId", "conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$scheduleId": {
        ".read": "auth != null && (root.child('buses').child(data.child('busId').val()).child('ownerId').val() == auth.uid || data.child('conductorId').val() == auth.uid)",
        ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
        ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber()"
      }
    }
  }
}










2222222





{
  "rules": {
    "users": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
        "phone": {
          ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
        },
        "age": {
          ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
        },
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
        },
        "ownerId": {
          ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
        }
      }
    },
    "buses": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$busId": {
        ".read": "auth != null && (data.child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
        ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'fares', 'createdAt'])",
        "fares": {
          "$stop": {
            ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
            "$dest": {
              ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
            }
          }
        }
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
        ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
      }
    },
 "schedules": {
  ".indexOn": ["busId", "conductorId"],
  ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
  "$scheduleId": {
    ".read": "auth != null && (root.child('buses').child(data.child('busId').val()).child('ownerId').val() == auth.uid || data.child('conductorId').val() == auth.uid)",
    ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
    ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber()"
  }
},
"requests": {
  ".indexOn": ["status", "riderId", "acceptedBy"],
  "$id": {
    ".read": "auth != null && (data.child('riderId').val() == auth.uid || data.child('acceptedBy').val() == auth.uid)",
    ".write": "auth != null && (
      (newData.child('riderId').val() == auth.uid && (!data.exists() || data.child('status').val() == 'Pending')) ||
      (root.child('users').child(auth.uid).child('role').val() == 'Conductor')
    )",
    ".validate": "newData.hasChildren(['id', 'riderId', 'pickup', 'pickupLat', 'pickupLng', 'destination', 'destLat', 'destLng', 'seats', 'status', 'createdAt'])"
  }
},
"locations": {
  ".indexOn": ["timestamp"],
  "$conductorId": {
    ".read": "auth != null",
    ".write": "auth != null && auth.uid == $conductorId",
    ".validate": "newData.hasChildren(['lat', 'lng', 'timestamp'])"
  }
}


  }
}












333333





{
  "rules": {
    "users": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
        "phone": {
          ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
        },
        "age": {
          ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
        },
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
        },
        "ownerId": {
          ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
        }
      }
    },
    "buses": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null",
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
        ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'fares', 'createdAt'])",
        "fares": {
          "$stop": {
            ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
            "$dest": {
              ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
            }
          }
        }
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
        ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
      }
    },
    "schedules": {
      ".indexOn": ["busId", "conductorId"],
      ".read": "auth != null",
      "$scheduleId": {
        ".read": "auth != null",
        ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
        ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber()"
      }
    },
    "requests": {
      ".indexOn": ["status", "riderId", "acceptedBy", "busId"],
      ".read": "auth != null && (query.orderByChild == 'status' && query.equalTo == 'Pending' && root.child('users').child(auth.uid).child('role').val() == 'Conductor') || (query.orderByChild == 'riderId' && query.equalTo == auth.uid) || (query.orderByChild == 'acceptedBy' && query.equalTo == auth.uid)",
      "$id": {
        ".read": "auth != null && (data.child('riderId').val() == auth.uid || data.child('acceptedBy').val() == auth.uid || (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending'))",
        ".write": "auth != null && (
          (newData.child('riderId').val() == auth.uid && (!data.exists() || (data.child('status').val() == 'Pending' && data.child('riderId').val() == auth.uid))) ||
          (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Accepted')
        )",
        ".validate": "newData.hasChildren(['id', 'riderId', 'busId', 'pickup', 'destination', 'pickupLatLng', 'destinationLatLng', 'seats', 'fare', 'status', 'createdAt']) &&
          newData.child('pickup').isString() &&
          newData.child('destination').isString() &&
          newData.child('pickupLatLng').hasChildren(['lat', 'lng']) &&
          newData.child('destinationLatLng').hasChildren(['lat', 'lng']) &&
          newData.child('seats').isNumber() && newData.child('seats').val() > 0 &&
          newData.child('fare').isNumber() && newData.child('fare').val() >= 0 &&
          newData.child('status').isString() && (newData.child('status').val() == 'Pending' || newData.child('status').val() == 'Accepted' || newData.child('status').val() == 'Cancelled') &&
          (!newData.child('otp').exists() || newData.child('otp').isString()) &&
          (!newData.child('conductorId').exists() || newData.child('conductorId').isString()) &&
          (!newData.child('acceptedBy').exists() || newData.child('acceptedBy').isString())"
      }
    },
    "conductorLocations": {
      ".indexOn": ["timestamp"],
      "$conductorId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $conductorId",
        ".validate": "newData.hasChildren(['lat', 'lng', 'timestamp']) &&
          newData.child('lat').isNumber() &&
          newData.child('lng').isNumber() &&
          newData.child('timestamp').isNumber()"
      }
    }
  }
}



23/10/25


{
  "rules": {
    "users": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
        "phone": {
          ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
        },
        "age": {
          ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
        },
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
        },
        "ownerId": {
          ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
        }
      }
    },
    "buses": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null",
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
        ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'fares', 'createdAt'])",
        "fares": {
          "$stop": {
            ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
            "$dest": {
              ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
            }
          }
        }
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
        ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
      }
    },
   "schedules": {
  ".indexOn": ["busId", "conductorId"],
  ".read": "auth != null",
  "$scheduleId": {
    ".read": "auth != null",
    ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
    ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'endTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('endTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber()"
  }
},
    "requests": {
      ".indexOn": ["status", "riderId", "acceptedBy", "busId"],
      ".read": "auth != null && (query.orderByChild == 'status' && query.equalTo == 'Pending' && root.child('users').child(auth.uid).child('role').val() == 'Conductor') || (query.orderByChild == 'riderId' && query.equalTo == auth.uid) || (query.orderByChild == 'acceptedBy' && query.equalTo == auth.uid)",
      "$id": {
        ".read": "auth != null && (data.child('riderId').val() == auth.uid || data.child('acceptedBy').val() == auth.uid || (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending'))",
        ".write": "auth != null && (
          (newData.child('riderId').val() == auth.uid && (!data.exists() || (data.child('status').val() == 'Pending' && data.child('riderId').val() == auth.uid))) ||
          (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Accepted' && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid) ||
          (newData.child('riderId').val() == auth.uid && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Cancelled')
        )",
        ".validate": "newData.hasChildren(['id', 'riderId', 'pickup', 'destination', 'pickupLatLng', 'destinationLatLng', 'seats', 'fare', 'status', 'createdAt']) &&
          (newData.child('status').val() == 'Pending' || newData.hasChild('busId')) &&
          (!newData.hasChild('busId') || newData.child('busId').isString()) &&
          newData.child('pickup').isString() &&
          newData.child('destination').isString() &&
          newData.child('pickupLatLng').hasChildren(['lat', 'lng']) &&
          newData.child('destinationLatLng').hasChildren(['lat', 'lng']) &&
          newData.child('seats').isNumber() && newData.child('seats').val() > 0 &&
          newData.child('fare').isNumber() && newData.child('fare').val() >= 0 &&
          newData.child('status').isString() && (newData.child('status').val() == 'Pending' || newData.child('status').val() == 'Accepted' || newData.child('status').val() == 'Cancelled') &&
          (!newData.child('otp').exists() || newData.child('otp').isString()) &&
          (!newData.child('conductorId').exists() || newData.child('conductorId').isString()) &&
          (!newData.child('acceptedBy').exists() || newData.child('acceptedBy').isString())"
      }
    },
    "conductorLocations": {
      ".indexOn": ["timestamp"],
      "$conductorId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $conductorId",
        ".validate": "newData.hasChildren(['lat', 'lng', 'timestamp']) &&
          newData.child('lat').isNumber() &&
          newData.child('lng').isNumber() &&
          newData.child('timestamp').isNumber()"
      }
    }
  }
}









after implementing chat message


{
  "rules": {
    "users": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
      "$uid": {
        ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
        ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
        ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
        "phone": {
          ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
        },
        "age": {
          ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
        },
        "email": {
          ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
        },
        "role": {
          ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
        },
        "ownerId": {
          ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
        }
      }
    },
    "buses": {
      ".indexOn": ["ownerId"],
      ".read": "auth != null",
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
        ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'route', 'fares', 'createdAt'])",
        "fares": {
          "$stop": {
            ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
            "$dest": {
              ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
            }
          }
        },
        "route": {
          ".validate": "newData.child('originLoc').val() !== null && newData.child('destinationLoc').val() !== null"
        }
      }
    },
    "busAssignments": {
      ".indexOn": ["conductorId"],
      ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
      "$busId": {
        ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
        ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
        ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
      }
    },
    "schedules": {
      ".indexOn": ["busId", "conductorId"],
      ".read": "auth != null",
      "$scheduleId": {
        ".read": "auth != null",
        ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
        ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'endTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('endTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber() && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == newData.child('conductorId').val()"
      }
    },
    "requests": {
      ".indexOn": ["status", "riderId", "acceptedBy", "busId"],
      ".read": "auth != null && (query.orderByChild == 'status' && query.equalTo == 'Pending' && root.child('users').child(auth.uid).child('role').val() == 'Conductor') || (query.orderByChild == 'riderId' && query.equalTo == auth.uid) || (query.orderByChild == 'acceptedBy' && query.equalTo == auth.uid)",
      "$id": {
        ".read": "auth != null && (data.child('riderId').val() == auth.uid || data.child('acceptedBy').val() == auth.uid || (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending'))",
        ".write": "auth != null && ((newData.child('riderId').val() == auth.uid && (!data.exists() || (data.child('status').val() == 'Pending' && data.child('riderId').val() == auth.uid))) || (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Accepted' && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid) || (newData.child('riderId').val() == auth.uid && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Cancelled'))",
        ".validate": "newData.hasChildren(['id', 'riderId', 'pickup', 'destination', 'pickupLatLng', 'destinationLatLng', 'seats', 'fare', 'status', 'createdAt']) && (newData.child('status').val() == 'Pending' || newData.hasChild('busId')) && (!newData.hasChild('busId') || newData.child('busId').isString()) && newData.child('pickup').isString() && newData.child('destination').isString() && newData.child('pickupLatLng').hasChildren(['lat', 'lng']) && newData.child('destinationLatLng').hasChildren(['lat', 'lng']) && newData.child('seats').isNumber() && newData.child('seats').val() > 0 && newData.child('fare').isNumber() && newData.child('fare').val() >= 0 && newData.child('status').isString() && (newData.child('status').val() == 'Pending' || newData.child('status').val() == 'Accepted' || newData.child('status').val() == 'Cancelled') && (!newData.child('otp').exists() || newData.child('otp').isString()) && (!newData.child('conductorId').exists() || newData.child('conductorId').isString()) && (!newData.child('acceptedBy').exists() || newData.child('acceptedBy').isString()) && (!newData.child('scheduleId').exists() || newData.child('scheduleId').isString()) && (!newData.child('acceptedAt').exists() || newData.child('acceptedAt').isNumber())"
      }
    },
    "conductorLocations": {
      ".indexOn": ["timestamp"],
      "$conductorId": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $conductorId",
        ".validate": "newData.hasChildren(['lat', 'lng', 'timestamp']) && newData.child('lat').isNumber() && newData.child('lng').isNumber() && newData.child('timestamp').isNumber()"
      }
    },
    "messages": {  // NEW: Chat messages node
      "$requestId": {
        ".read": "auth != null && root.child('requests').child($requestId).child('status').val() == 'Accepted' && (root.child('requests').child($requestId).child('riderId').val() == auth.uid || root.child('requests').child($requestId).child('conductorId').val() == auth.uid) && now <= root.child('schedules').child(root.child('requests').child($requestId).child('scheduleId').val()).child('endTime').val() + 432000000",
        ".write": "false",
        "messages": {
          "$messageId": {
            ".write": "auth != null && root.child('requests').child($requestId).child('status').val() == 'Accepted' && (root.child('requests').child($requestId).child('riderId').val() == auth.uid || root.child('requests').child($requestId).child('conductorId').val() == auth.uid) && newData.child('senderId').val() == auth.uid && now <= root.child('schedules').child(root.child('requests').child($requestId).child('scheduleId').val()).child('endTime').val() + 432000000",
            ".validate": "newData.hasChildren(['senderId', 'text', 'timestamp']) && newData.child('senderId').isString() && newData.child('text').isString() && newData.child('timestamp').isNumber()"
          }
        }
      }
    }
  }
}


31 Oct 2025

{
    "rules": {
    "users": {
    ".indexOn": ["ownerId"],
    ".read": "auth != null && root.child('users').child(auth.uid).child('role').val() == 'Owner'",
    "$uid": {
    ".read": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && data.child('ownerId').val() == auth.uid))",
    ".write": "auth != null && (auth.uid == $uid || (root.child('users').child(auth.uid).child('role').val() == 'Owner' && newData.child('role').val() == 'Conductor'))",
    ".validate": "newData.hasChildren(['email', 'name', 'phone', 'age', 'role', 'createdAt'])",
    "phone": {
    ".validate": "newData.val().matches(/^(\\+8801|01)[3-9]\\d{8}$/)"
},
    "age": {
    ".validate": "newData.isNumber() && newData.val() >= 18 && newData.val() <= 100"
},
    "email": {
    ".validate": "newData.isString() && newData.val().matches(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$/)"
},
    "role": {
    ".validate": "newData.isString() && (newData.val() == 'Rider' || newData.val() == 'Conductor' || newData.val() == 'Owner')"
},
    "ownerId": {
    ".validate": "newData.val() == null || (newData.isString() && root.child('users').child(newData.val()).child('role').val() == 'Owner')"
}
}
},
    "buses": {
    ".indexOn": ["ownerId"],
    ".read": "auth != null",
    "$busId": {
    ".read": "auth != null",
    ".write": "auth != null && (newData.child('ownerId').val() == auth.uid || (data.child('ownerId').val() == auth.uid && !newData.exists()))",
    ".validate": "newData.hasChildren(['busId', 'ownerId', 'name', 'number', 'fitnessCertificate', 'taxToken', 'stops', 'route', 'fares', 'createdAt'])",
    "fares": {
    "$stop": {
    ".validate": "$stop.matches(/^[A-Za-z0-9 ]+$/) && newData.hasChildren()",
    "$dest": {
    ".validate": "$dest.matches(/^[A-Za-z0-9 ]+$/) && newData.isNumber() && newData.val() >= 0"
}
}
},
    "route": {
    ".validate": "newData.child('originLoc').val() !== null && newData.child('destinationLoc').val() !== null"
}
}
},
    "busAssignments": {
    ".indexOn": ["conductorId"],
    ".read": "auth != null && (root.child('users').child(auth.uid).child('role').val() == 'Owner' || (query.orderByChild == 'conductorId' && query.equalTo == auth.uid))",
    "$busId": {
    ".read": "auth != null && (root.child('buses').child($busId).child('ownerId').val() == auth.uid || root.child('busAssignments').child($busId).child('conductorId').val() == auth.uid)",
    ".write": "auth != null && root.child('buses').child($busId).child('ownerId').val() == auth.uid",
    ".validate": "newData.hasChild('conductorId') && newData.child('conductorId').isString()"
}
},
    "schedules": {
    ".indexOn": ["busId", "conductorId"],
    ".read": "auth != null",
    "$scheduleId": {
    ".read": "auth != null",
    ".write": "auth != null && newData.child('conductorId').val() == auth.uid && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid",
    ".validate": "newData.hasChildren(['busId', 'conductorId', 'startTime', 'endTime', 'date', 'createdAt']) && newData.child('startTime').isNumber() && newData.child('endTime').isNumber() && newData.child('date').isString() && newData.child('createdAt').isNumber() && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == newData.child('conductorId').val()"
}
},
    "requests": {
    ".indexOn": ["status", "riderId", "acceptedBy", "busId"],
    ".read": "auth != null && (query.orderByChild == 'status' && query.equalTo == 'Pending' && root.child('users').child(auth.uid).child('role').val() == 'Conductor') || (query.orderByChild == 'riderId' && query.equalTo == auth.uid) || (query.orderByChild == 'acceptedBy' && query.equalTo == auth.uid)",
    "$id": {
    ".read": "auth != null && (data.child('riderId').val() == auth.uid || data.child('acceptedBy').val() == auth.uid || (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending'))",
    ".write": "auth != null && ((newData.child('riderId').val() == auth.uid && (!data.exists() || (data.child('status').val() == 'Pending' && data.child('riderId').val() == auth.uid))) || (root.child('users').child(auth.uid).child('role').val() == 'Conductor' && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Accepted' && root.child('busAssignments').child(newData.child('busId').val()).child('conductorId').val() == auth.uid) || (newData.child('riderId').val() == auth.uid && data.child('status').val() == 'Pending' && newData.child('status').val() == 'Cancelled'))",
    ".validate": "newData.hasChildren(['id', 'riderId', 'pickup', 'destination', 'pickupLatLng', 'destinationLatLng', 'seats', 'fare', 'status', 'createdAt']) && (newData.child('status').val() == 'Pending' || newData.hasChild('busId')) && (!newData.hasChild('busId') || newData.child('busId').isString()) && newData.child('pickup').isString() && newData.child('destination').isString() && newData.child('pickupLatLng').hasChildren(['lat', 'lng']) && newData.child('destinationLatLng').hasChildren(['lat', 'lng']) && newData.child('seats').isNumber() && newData.child('seats').val() > 0 && newData.child('fare').isNumber() && newData.child('fare').val() >= 0 && newData.child('status').isString() && (newData.child('status').val() == 'Pending' || newData.child('status').val() == 'Accepted' || newData.child('status').val() == 'Cancelled') && (!newData.child('otp').exists() || newData.child('otp').isString()) && (!newData.child('conductorId').exists() || newData.child('conductorId').isString()) && (!newData.child('acceptedBy').exists() || newData.child('acceptedBy').isString()) && (!newData.child('scheduleId').exists() || newData.child('scheduleId').isString()) && (!newData.child('acceptedAt').exists() || newData.child('acceptedAt').isNumber())"
}
},
    "conductorLocations": {
    ".indexOn": ["timestamp"],
    "$conductorId": {
    ".read": "auth != null",
    ".write": "auth != null && auth.uid == $conductorId",
    ".validate": "newData.hasChildren(['lat', 'lng', 'timestamp']) && newData.child('lat').isNumber() && newData.child('lng').isNumber() && newData.child('timestamp').isNumber()"
}
},
    "messages": {  // NEW: Chat messages node
    "$requestId": {
        ".read": "auth != null && root.child('requests').child($requestId).child('status').val() == 'Accepted' && (root.child('requests').child($requestId).child('riderId').val() == auth.uid || root.child('requests').child($requestId).child('conductorId').val() == auth.uid) && now <= root.child('schedules').child(root.child('requests').child($requestId).child('scheduleId').val()).child('endTime').val() + 432000000",
        ".write": "false",
        "messages": {
        "$messageId": {
        ".write": "auth != null && root.child('requests').child($requestId).child('status').val() == 'Accepted' && (root.child('requests').child($requestId).child('riderId').val() == auth.uid || root.child('requests').child($requestId).child('conductorId').val() == auth.uid) && newData.child('senderId').val() == auth.uid && now <= root.child('schedules').child(root.child('requests').child($requestId).child('scheduleId').val()).child('endTime').val() + 432000000",
        ".validate": "newData.hasChildren(['senderId', 'text', 'timestamp']) && newData.child('senderId').isString() && newData.child('text').isString() && newData.child('timestamp').isNumber()"
    }
    }
    }
}
}








 */
}

