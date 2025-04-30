<!--
	@component
	A FolderStructure component where one option can be selected.
-->

<script lang="ts">
    import { equals } from '$lib/duui/utils/text'
    import type { Placement } from '@floating-ui/dom'
    import {faFolder, type IconDefinition} from '@fortawesome/free-solid-svg-icons'
    import { popup, type PopupSettings } from '@skeletonlabs/skeleton'
    import { RecursiveTreeView, type TreeViewNode } from '@skeletonlabs/skeleton'
    import FolderIcon from "$lib/svelte/components/FolderIcon.svelte";

    import Fa from 'svelte-fa'

    export let label: string = ''
    export let name: string = label
    export let isMultiple = false

    export let value: string = ""

    export let placement: Placement = 'bottom-start'

    export let offset: number = 4

    export let tree: TreeViewNode | null = null

    // export let style: string = 'input-wrapper'
    // export let rounded: string = 'rounded-md'
    // export let border: string = 'border'
    // export let textAlign: string = 'text-start'

    let icon: IconDefinition = faFolder

    const dropdown: PopupSettings = {
        event: 'click',
        target: name,
        placement: placement,
        closeQuery: '.listbox-item',
        middleware: {
            offset: offset
        }
    }

    let isStart = true

    let checkedNodes : string[] = value?.length > 0 ? value.toString().split(",") : []
    let prevNode: string = ""
    let indeterminateNodes: string[] = []
    let parentNodes = {}



    let myTreeViewNodes: TreeViewNode[] = [tree]
    let addFolderIcon = (node: TreeViewNode, height: number) => {
        node.lead = FolderIcon
        if (node.children) {
            for (let child of node.children) {
                parentNodes[child.id] = node.id
                addFolderIcon(child, height + 1)
            }
        }
    }

    let getNode = (nodes: TreeViewNode[], id: string) => {
        let getNode1 = (node: TreeViewNode) => {
            if (node.id === id) return node;

            if (node.children) {
                for (let child of node.children) {
                    let result = getNode1(child)
                    if (result) return result
                }
            }
            return null;
        }

        for (let node of nodes) {
            let result = getNode1(node)
            if (result) return result
        }
        return null;
    }

    for (let node of myTreeViewNodes) {
        parentNodes[node.id] = node.id
        addFolderIcon(node, 1)

    }

    if (checkedNodes.length > 0) {
        for (let node of checkedNodes) {
            if (!checkedNodes.includes(parentNodes[node])) {
                indeterminateNodes.push(parentNodes[node])
            }
        }
    }

    let displayCheckedNodes = (nodes: string[], inde: string[]) => {
        let v:string


        if (isMultiple) {

            if (nodes.length === 0 && value?.length > 0) {
                nodes = value.toString().split(",")
                v = nodes.map((x) => getNode(myTreeViewNodes, x)).filter(x => x !== null).map(x => x.content).join(", ")
            } else {

                let isInIndeterminates = (id) => {
                    return inde.includes(id)
                }

                let filtered = nodes
                    .filter((x) => parentNodes[x] === x || isInIndeterminates(parentNodes[x]))

                value = filtered.join(",")
                v = filtered.map((x) => getNode(myTreeViewNodes, x).content).join(", ")
            }

        }

        else {

            if (nodes.length === 0 && value !== "") {
                nodes = value.toString().split(",")
                const tmp = getNode(myTreeViewNodes, value)
                v =  tmp ? tmp.content : ""
            } else {

                if (nodes.length > 0) {
                    if (nodes.length > 1 && equals(prevNode, nodes[0])) nodes.shift()
                    if (nodes.length > 1 && equals(prevNode, nodes[1])) nodes.pop()
                    prevNode = nodes[0]
                }
                value = nodes.join(",")
                v = nodes.map((x) => getNode(myTreeViewNodes, x).content).join(", ")

            }
        }

        isStart = false;
        return v;
    }

    $: displayedCheckedNodes = displayCheckedNodes(checkedNodes, indeterminateNodes)

    // {
    //     console.log("checkedNodes", checkedNodes)
    //     console.log("indeterminateNodes", indeterminateNodes)
    // }

</script>

<div class="label flex flex-col md:min-w-[220px]">
    {#if label}
        <span class="form-label text-start">{label} </span>
    {/if}
    <button
            class="flex items-center !justify-between gap-2 px-3 py-2 leading-6 border rounded-md input-wrapper"
            use:popup={dropdown}
    >
        <span>{ displayedCheckedNodes }</span>
        <Fa {icon} />
    </button>
</div>
<div data-popup={name} class="fixed">
    <div class="overflow-scroll max-h-96 popup-solid p-1 md:min-w-[768px]">
        <!--TODO: Allow non-recursive selection by turning relation={false} -->
        <RecursiveTreeView
            selection
            multiple={isMultiple}
            relational={isMultiple}
            bind:checkedNodes={checkedNodes}
            bind:indeterminateNodes={indeterminateNodes}

            nodes={myTreeViewNodes}

        />

    </div>

</div>
