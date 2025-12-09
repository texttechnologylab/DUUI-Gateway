<!--
	@component
	Layered editor for DUUI components with drag-and-drop restricted to each layer.
-->
<script lang="ts">
	import { dndzone, type DndEvent } from 'svelte-dnd-action'
	import { buildGraph, computeLayers, type Dependency, type DUUIComponent } from '$lib/duui/component'
	import PipelineComponent from '$lib/svelte/components/PipelineComponent.svelte'
	import ComponentPopup from '$lib/svelte/components/ComponentPopup.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'
	import { componentDrawerSettings } from '$lib/config'
	import { getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { createEventDispatcher, afterUpdate, onMount, onDestroy } from 'svelte'
	import { flip } from 'svelte/animate'
	import { errorToast } from '$lib/duui/utils/ui'
	import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'

	const drawerStore = getDrawerStore()
	const toastStore = getToastStore()
	const dispatch = createEventDispatcher<{ reorder: { components: DUUIComponent[] } }>()

	export let components: DUUIComponent[] = []
	export let templateComponents: DUUIComponent[] = []

	// snapshot of the components array before a drag starts
	let snapshot: DUUIComponent[] | null = null

	// current layer mapping for visualization / validation
	let layerById: Map<string, number> = new Map()

	let cycleError = ''

	type EdgePath = {
		fromId: string
		toId: string
		d: string
	}

	let edgePaths: EdgePath[] = []
	let container: HTMLDivElement

	const recalcGraphAndEdges = (source?: DUUIComponent[]) => {

		const comps = source ?? (components ?? [])

		if (comps.length === 0) {
			layerById = new Map()
			edgePaths = []
			cycleError = ''

			return;
		} 

		try {
			const { edges } = buildGraph(comps)
			const { layer } = computeLayers(comps, edges)
			layerById = layer
			recomputeEdgePaths(comps, edges, layer)
			cycleError = ''
		} catch (err) {
			const message = err instanceof Error ? err.message : String(err)
			if (message.includes('Graph has a cycle')) {
				cycleError = 'Pipeline has a cycle in annotation dependencies.'
			} else {
				cycleError = ''
			}
			// fall back: all in layer 0, no edges
			layerById = new Map(comps.map((c) => [c.id, 0]))
			edgePaths = []
		}
	}

	// derived view order: sort by layer, then original index
	let orderedComponents: DUUIComponent[] = []
	$: {
		const comps = components ?? []
		orderedComponents = [...comps].sort((a, b) => {
			const la = layerById.get(a.id) ?? 0
			const lb = layerById.get(b.id) ?? 0
			if (la !== lb) return la - lb
			return (a.index ?? 0) - (b.index ?? 0)
		})
	}

	// recompute layers whenever components change
	$: {
		const comps = components ?? []
		
		setTimeout(() => recalcGraphAndEdges(comps), 0)
		setTimeout(() => recalcGraphAndEdges(comps), 100)
		setTimeout(() => recalcGraphAndEdges(comps), 500)
		setTimeout(() => recalcGraphAndEdges(comps), 1000)
	}

	const recomputeEdgePaths = (
		comps: DUUIComponent[],
		edges: Dependency[],
		layer: Map<string, number>
	) => {
		if (!container) {
			edgePaths = []
			return
		}

		const containerRect = container.getBoundingClientRect()

		const getAnchor = (el: HTMLElement, side: 'left' | 'right') => {
			const rect = el.getBoundingClientRect()
			const x = side === 'left' ? rect.left - containerRect.left : rect.right - containerRect.left
			const y = rect.top + rect.height / 2 - containerRect.top
			return { x, y }
		}

		const paths: EdgePath[] = []

		for (const edge of edges) {
			const fromId = edge.from
			const toId = edge.to

			for (const t of edge.types) {
				const fromChip =
					container.querySelector<HTMLElement>(
						`[data-connector="output"][data-component-id="${fromId}"][data-type="${t}"]`
					) ??
					container.querySelector<HTMLElement>(`[data-component-id="${fromId}"]`)

				const toChip =
					container.querySelector<HTMLElement>(
						`[data-connector="input"][data-component-id="${toId}"][data-type="${t}"]`
					) ??
					container.querySelector<HTMLElement>(`[data-component-id="${toId}"]`)

				if (!fromChip || !toChip) continue

				const { x: x1, y: y1 } = getAnchor(fromChip, 'right')
				const { x: x2, y: y2 } = getAnchor(toChip, 'left')

				const dx = x2 - x1 || 1
				const offset = dx * 0.3
				const cx1 = x1 + offset
				const cx2 = x2 - offset

				const d = `M ${x1} ${y1} C ${cx1} ${y1}, ${cx2} ${y2}, ${x2} ${y2}`
				paths.push({ fromId, toId, d })
			}
		}

		edgePaths = paths
	}

	let resizeObserver: ResizeObserver | null = null
	let resizeHandler: (() => void) | null = null

	onMount(() => {
		const recalc = () => recalcGraphAndEdges()

		recalc()

		if (typeof ResizeObserver !== 'undefined') {
		resizeObserver = new ResizeObserver(() => recalc())
			if (container) resizeObserver.observe(container)
		} else if (typeof window !== 'undefined') {
			resizeHandler = () => recalc()
			window.addEventListener('resize', resizeHandler)
		}
	})

	onDestroy(() => {
	if (resizeObserver && container) {
		resizeObserver.unobserve(container)
		resizeObserver.disconnect()
		resizeObserver = null
	}
	if (resizeHandler && typeof window !== 'undefined') {
		window.removeEventListener('resize', resizeHandler)
		resizeHandler = null
	}
})

	// afterUpdate(() => {
	// 	// keep curves in sync with DOM after DnD animations
	// 	recalcGraphAndEdges()
		
	// 	// setTimeout(recalcGraphAndEdges, 100)
	// 	// setTimeout(recalcGraphAndEdges, 500)
	// 	setTimeout(recalcGraphAndEdges, 1000)
	// })

	const handleDndConsider = (event: CustomEvent<DndEvent<DUUIComponent>>) => {
		if (!snapshot) {
			snapshot = [...orderedComponents]
		}
		orderedComponents = event.detail.items as DUUIComponent[]

		// setTimeout(recalcGraphAndEdges, 100)
		// setTimeout(recalcGraphAndEdges, 500)
		// setTimeout(recalcGraphAndEdges, 1000)
	}

	const handleDndFinalize = (event: CustomEvent<DndEvent<DUUIComponent>>) => {
		const newOrder = event.detail.items as DUUIComponent[]
		const base = snapshot ?? orderedComponents

		// recompute layering from the pre-drag state (order-independent)
		let currentLayerById: Map<string, number>
		try {
			const { edges } = buildGraph(base)
			const { layer } = computeLayers(base, edges)
			currentLayerById = layer
		} catch {
			currentLayerById = new Map(base.map((c) => [c.id, 0]))
		}

		const layersOrder = newOrder.map((c) => currentLayerById.get(c.id) ?? 0)
		const nonDecreasing = layersOrder.every(
			(v, i, arr) => i === 0 || arr[i - 1] <= v
		)

		if (!nonDecreasing && snapshot) {
			// invalid move across layers -> revert, show toast, and recompute edges after DOM settles
			const restored = snapshot
			orderedComponents = restored
			components = restored

			toastStore.trigger(
				errorToast('Cannot move component across dependency layers', 3000)
			)

			const recalc = () => {
				recalcGraphAndEdges(restored)
				dispatch('reorder', { components })
			}

			setTimeout(recalc, 100)
			// setTimeout(recalc, 500)
			// setTimeout(recalc, 1000)
			// if (typeof requestAnimationFrame !== 'undefined') {
			// 	requestAnimationFrame(recalc)
			// } else {
			// 	setTimeout(recalc, 0)
			// }

			snapshot = null
			return
		}

		const updated = newOrder.map((c, index) => ({ ...c, index }))
		orderedComponents = updated
		components = updated

		const recalc = () => {
			recalcGraphAndEdges(updated)
			dispatch('reorder', { components })
		}

		setTimeout(recalc, 100)
		// setTimeout(recalc, 500)
		// setTimeout(recalc, 1000)
		// if (typeof requestAnimationFrame !== 'undefined') {
		// 	requestAnimationFrame(recalc)
		// } else {
		// 	setTimeout(recalc, 0)
		// }

		snapshot = null
	}

	const cloneComponent = ({ component }: { component: DUUIComponent }) => {
		drawerStore.open({
			id: 'component',
			...componentDrawerSettings,
			meta: {
				component: {
					...component,
					name: component.name + ' - Copy',
					index: components.length
				},
				inEditor: false,
				creating: true
			}
		})
	}

	const deleteComponent = (oid: string) => {
		components = components.filter((c) => c.oid !== oid)
		dispatch('reorder', { components })
	}
</script>


<div class="relative min-h-[400px] space-y-8 isolate md:p-16" bind:this={container}>
	{#if cycleError} 
		<div class="md:max-w-5xl mx-auto">
			<Tip tipTheme="error" tipSize="lg" customIcon={faExclamationTriangle} >
				{ cycleError }
			</Tip>
		</div>
	{/if}
	<svg class="absolute inset-0 pointer-events-none z-10" width="100%" height="100%">
		{#each edgePaths as edge}
		<path
		d={edge.d}
		class="text-primary-500 opacity-80"
		stroke="currentColor"
		stroke-width="4"
				fill="none"
			/>
		{/each}
	</svg>
	<ul
		use:dndzone={{ items: orderedComponents, dropTargetStyle: {} }}
		on:consider={handleDndConsider}
		on:finalize={handleDndFinalize}
		class="grid md:max-w-5xl mx-auto !cursor-move p-20"
	>
		{#each orderedComponents as component, i (component.id)}
			<div
				animate:flip={{ duration: 300 }}
				class="relative !cursor-move m-5"
				data-layer={layerById.get(component.id) ?? 0}
				data-component-id={component.id}
			>
				<div class="relative !cursor-move">
					<PipelineComponent
						{component}
						cloneable={true}
						on:clone={(event) => cloneComponent(event.detail)}
						on:deleteComponent={(event) => deleteComponent(event.detail.oid)}
					/>
				</div>
				{#if i < orderedComponents.length - 1}
					<div
						class="my-4 mx-auto flex items-center justify-center
							relative
							before:absolute before:h-full before:w-1 before:-translate-x-1/2 before:left-1/2
							before:bg-surface-100-800-token before:-z-50 before:scale-y-[200%]
							"
					>
						<ComponentPopup {templateComponents} index={i + 1} />
					</div>
				{/if}
				<!-- <div class="divider my-4 border-t border-gray-300"></div> -->
			</div>
		{/each}
	</ul>
	<div class="mx-auto flex items-center justify-center">
		<ComponentPopup {templateComponents} />
	</div>
</div>
