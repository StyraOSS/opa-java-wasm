package armo_builtins
import rego.v1

# ---------------- Pod ----------------
deny contains msga if {
    pod := input[_]
    pod.kind == "Pod"
    container := pod.spec.containers[i]

    start_of_path := "spec"
    run_as_user_fixpath  := evaluate_workload_run_as_user(container, pod, start_of_path)
    run_as_group_fixpath := evaluate_workload_run_as_group(container, pod, start_of_path)

    all_fixpaths := array.concat(run_as_user_fixpath, run_as_group_fixpath)
    count(all_fixpaths) > 0

    fixPaths := get_fixed_paths(all_fixpaths, i)

    msga := {
        "alertMessage": concat("", [
            "container: ", container.name,
            " in pod: ",   pod.metadata.name,
            " may run as root",
        ]),
        "packagename": "armo_builtins",
        "alertScore": 7,
        "reviewPaths": [],
        "failedPaths": [],
        "fixPaths": fixPaths,
        "alertObject": { "k8sApiObjects": [pod] },
    }
}

# -------- Deployment / RS / DS / SS / Job --------
deny contains msga if {
    wl := input[_]
    kinds := {"Deployment","ReplicaSet","DaemonSet","StatefulSet","Job"}
    kinds[wl.kind]

    container := wl.spec.template.spec.containers[i]
    start_of_path := "spec.template.spec"

    run_as_user_fixpath  := evaluate_workload_run_as_user(container, wl.spec.template, start_of_path)
    run_as_group_fixpath := evaluate_workload_run_as_group(container, wl.spec.template, start_of_path)

    all_fixpaths := array.concat(run_as_user_fixpath, run_as_group_fixpath)
    count(all_fixpaths) > 0
    fixPaths := get_fixed_paths(all_fixpaths, i)

    msga := {
        "alertMessage": concat("", [
            "container: ", container.name,
            " in ", wl.kind, ": ", wl.metadata.name,
            " may run as root",
        ]),
        "packagename": "armo_builtins",
        "alertScore": 7,
        "reviewPaths": [],
        "failedPaths": [],
        "fixPaths": fixPaths,
        "alertObject": { "k8sApiObjects": [wl] },
    }
}

# ---------------- CronJob ----------------
deny contains msga if {
    wl := input[_]
    wl.kind == "CronJob"

    container := wl.spec.jobTemplate.spec.template.spec.containers[i]
    start_of_path := "spec.jobTemplate.spec.template.spec"

    run_as_user_fixpath  := evaluate_workload_run_as_user(container, wl.spec.jobTemplate.spec.template, start_of_path)
    run_as_group_fixpath := evaluate_workload_run_as_group(container, wl.spec.jobTemplate.spec.template, start_of_path)

    all_fixpaths := array.concat(run_as_user_fixpath, run_as_group_fixpath)
    count(all_fixpaths) > 0
    fixPaths := get_fixed_paths(all_fixpaths, i)

    msga := {
        "alertMessage": concat("", [
            "container: ", container.name,
            " in ", wl.kind, ": ", wl.metadata.name,
            " may run as root",
        ]),
        "packagename": "armo_builtins",
        "alertScore": 7,
        "reviewPaths": [],
        "failedPaths": [],
        "fixPaths": fixPaths,
        "alertObject": { "k8sApiObjects": [wl] },
    }
}

get_fixed_paths(all_fixpaths, i) = res if {
    count(all_fixpaths) == 2
    idx := format_int(i, 10)
    res := [
        {
            "path": replace(all_fixpaths[0].path, "container_ndx", idx),
            "value": all_fixpaths[0].value,
        },
        {
            "path": replace(all_fixpaths[1].path, "container_ndx", idx),
            "value": all_fixpaths[1].value,
        },
    ]
} else = res if {
    idx := format_int(i, 10)
    res := [
        {
            "path": replace(all_fixpaths[0].path, "container_ndx", idx),
            "value": all_fixpaths[0].value,
        },
    ]
}

evaluate_workload_run_as_user(container, pod, start_of_path) = fixPath if {
    runAsNonRootVal := get_run_as_non_root_value(container, pod, start_of_path)
    runAsNonRootVal.value == false

    runAsUserVal := get_run_as_user_value(container, pod, start_of_path)
    runAsUserVal.value == 0

    alertInfo := choose_first_if_defined(runAsUserVal, runAsNonRootVal)
    fixPath := alertInfo.fixPath
} else = []                                 # <- «иначе» — пустой список

evaluate_workload_run_as_group(container, pod, start_of_path) = fixPath if {
    runAsGroupVal := get_run_as_group_value(container, pod, start_of_path)
    runAsGroupVal.value == 0
    fixPath := runAsGroupVal.fixPath
} else = []

get_run_as_non_root_value(container, pod, start_of_path) = v if {
    v := {
        "value": container.securityContext.runAsNonRoot,
        "fixPath": [{
            "path": concat("", [
                start_of_path,
                ".containers[container_ndx].securityContext.runAsNonRoot",
            ]),
            "value": "true",
        }],
        "defined": true,
    }
} else = v if {
    v := {
        "value": pod.spec.securityContext.runAsNonRoot,
        "fixPath": [{
            "path": concat("", [
                start_of_path,
                ".containers[container_ndx].securityContext.runAsNonRoot",
            ]),
            "value": "true",
        }],
        "defined": true,
    }
} else = {
    "value": false,
    "fixPath": [{
        "path": concat("", [
            start_of_path,
            ".containers[container_ndx].securityContext.runAsNonRoot",
        ]),
        "value": "true",
    }],
    "defined": false,
}

get_run_as_user_value(container, pod, start_of_path) = v if {
    path := concat("", [
        start_of_path,
        ".containers[container_ndx].securityContext.runAsUser",
    ])
    v := {
        "value": container.securityContext.runAsUser,
        "fixPath": [{ "path": path, "value": "1000" }],
        "defined": true,
    }
} else = v if {
    path := concat("", [start_of_path, ".securityContext.runAsUser"])
    v := {
        "value": pod.spec.securityContext.runAsUser,
        "fixPath": [{ "path": path, "value": "1000" }],
        "defined": true,
    }
} else = {
    "value": 0,
    "fixPath": [{
        "path": concat("", [
            start_of_path,
            ".containers[container_ndx].securityContext.runAsNonRoot",
        ]),
        "value": "true",
    }],
    "defined": false,
}

get_run_as_group_value(container, pod, start_of_path) = v if {
    path := concat("", [
        start_of_path,
        ".containers[container_ndx].securityContext.runAsGroup",
    ])
    v := {
        "value": container.securityContext.runAsGroup,
        "fixPath": [{ "path": path, "value": "1000" }],
        "defined": true,
    }
} else = v if {
    path := concat("", [start_of_path, ".securityContext.runAsGroup"])
    v := {
        "value": pod.spec.securityContext.runAsGroup,
        "fixPath": [{ "path": path, "value": "1000" }],
        "defined": true,
    }
} else = {
    "value": 0,
    "fixPath": [{
        "path": concat("", [
            start_of_path,
            ".containers[container_ndx].securityContext.runAsGroup",
        ]),
        "value": "1000",
    }],
    "defined": false,
}

choose_first_if_defined(l1, l2) = c if {
    l1.defined
    c := l1
} else = l2
