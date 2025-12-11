<!--
	@component
	Layered editor for DUUI components with a zoomable & pannable canvas.
-->
<script lang="ts">
	import { buildGraph, computeLayers, type Dependency, type DUUIComponent } from '$lib/duui/component'
	import PipelineComponent from '$lib/svelte/components/PipelineComponent.svelte'
	import ComponentPopup from '$lib/svelte/components/ComponentPopup.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'
	import { componentDrawerSettings } from '$lib/config'
	import { getDrawerStore } from '@skeletonlabs/skeleton'
	import { createEventDispatcher, onMount, onDestroy } from 'svelte'
	import { flip } from 'svelte/animate'
	import { faExclamationTriangle, faMinus, faPlus } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'

	const drawerStore = getDrawerStore()
	const dispatch = createEventDispatcher<{ reorder: { components: DUUIComponent[] } }>()

	export let components: DUUIComponent[] = []
	export let templateComponents: DUUIComponent[] = []

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
	let viewport: HTMLDivElement

	// zoom / pan state for the canvas
	let scale = 1
	const MIN_SCALE = 0.5
	const MAX_SCALE = 2
	let panX = 0
	let panY = 0
	let isPanning = false
	let lastPan = { x: 0, y: 0 }

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

	type LayerItem = { component: DUUIComponent; orderIndex: number }
	type LayerGroup = { layer: number; items: LayerItem[] }

	let layers: LayerGroup[] = []

	// group components by computed layer for horizontal alignment
	$: {
		const groups = new Map<number, LayerItem[]>()

		orderedComponents.forEach((component, orderIndex) => {
			const layer = layerById.get(component.id) ?? 0
			const list = groups.get(layer) ?? []
			list.push({ component, orderIndex })
			groups.set(layer, list)
		})

		layers = Array.from(groups.entries())
			.sort(([a], [b]) => a - b)
			.map(([layer, items]) => ({ layer, items }))
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
		const currentScale = scale || 1

		const getAnchor = (el: HTMLElement, side: 'left' | 'right') => {
			const rect = el.getBoundingClientRect()
			// center X for both, different vertical anchor:
			// - 'right'  (fromChip): bottom center
			// - 'left'   (toChip):   top center
			const xRaw = rect.left + rect.width / 2 - containerRect.left
			const yRaw =
				side === 'right'
					? rect.bottom - containerRect.top
					: rect.top - containerRect.top
			const x = xRaw / currentScale
			const y = yRaw / currentScale
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

				// "Hanging" S-curve: leave from-chip vertically downward,
				// travel through a lowered midline, then approach to-chip vertically upward.
				const verticalPadding = 40
				const midY = (y1 + y2) / 2 + verticalPadding

				const cx1x = x1
				const cx1y = midY
				const cx2x = x2
				const cx2y = midY

				const d = `M ${x1} ${y1} C ${cx1x} ${cx1y}, ${cx2x} ${cx2y}, ${x2} ${y2}`
				paths.push({ fromId, toId, d })
			}
		}

		edgePaths = paths
	}

let resizeObserver: ResizeObserver | null = null
let resizeHandler: (() => void) | null = null
let mouseMoveHandler: ((event: MouseEvent) => void) | null = null
let mouseUpHandler: ((event: MouseEvent) => void) | null = null
let panZoomEdgeTimeout: ReturnType<typeof setTimeout> | null = null
let hoveredComponentId: string | null = null

	const scheduleEdgeRecalc = () => {
		if (panZoomEdgeTimeout) {
			clearTimeout(panZoomEdgeTimeout)
		}
		panZoomEdgeTimeout = setTimeout(() => {
			recalcGraphAndEdges()
		}, 100)
	}

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

		if (typeof window !== 'undefined') {
			mouseMoveHandler = (event: MouseEvent) => {
				if (!isPanning) return
				const dx = event.clientX - lastPan.x
				const dy = event.clientY - lastPan.y
				panX += dx
				panY += dy
				lastPan = { x: event.clientX, y: event.clientY }
				scheduleEdgeRecalc()
			}

			mouseUpHandler = () => {
				isPanning = false
			}

			window.addEventListener('mousemove', mouseMoveHandler)
			window.addEventListener('mouseup', mouseUpHandler)
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
		if (mouseMoveHandler && typeof window !== 'undefined') {
			window.removeEventListener('mousemove', mouseMoveHandler)
			mouseMoveHandler = null
		}
	if (mouseUpHandler && typeof window !== 'undefined') {
		window.removeEventListener('mouseup', mouseUpHandler)
		mouseUpHandler = null
	}
	if (panZoomEdgeTimeout) {
		clearTimeout(panZoomEdgeTimeout)
		panZoomEdgeTimeout = null
	}
})

	const zoomBy = (factor: number) => {
		if (!viewport) return

		const rect = viewport.getBoundingClientRect()
		const cx = rect.width / 2
		const cy = rect.height / 2

		const prevScale = scale
		let next = prevScale * factor
		if (next < MIN_SCALE) next = MIN_SCALE
		if (next > MAX_SCALE) next = MAX_SCALE
		if (next === prevScale) return

		const worldX = (cx - panX) / prevScale
		const worldY = (cy - panY) / prevScale

		scale = next
		panX = cx - worldX * scale
		panY = cy - worldY * scale
		scheduleEdgeRecalc()
	}

	const zoomIn = () => zoomBy(1.1)
	const zoomOut = () => zoomBy(1 / 1.1)

	const handleWheel = (event: WheelEvent) => {
		event.preventDefault()

		if (!viewport) return

		const rect = viewport.getBoundingClientRect()
		const cx = event.clientX - rect.left
		const cy = event.clientY - rect.top

		const prevScale = scale
		const delta = -event.deltaY * 0.001
		let next = prevScale + delta
		if (next < MIN_SCALE) next = MIN_SCALE
		if (next > MAX_SCALE) next = MAX_SCALE

		if (next === prevScale) return

		const worldX = (cx - panX) / prevScale
		const worldY = (cy - panY) / prevScale

		scale = next
		panX = cx - worldX * scale
		panY = cy - worldY * scale
		scheduleEdgeRecalc()
	}

	const handleCanvasMouseDown = (event: MouseEvent) => {
		if (event.button !== 0) return
		const target = event.target as HTMLElement
		if (target.closest('button, a, input, textarea, [data-no-pan]')) return
		isPanning = true
		lastPan = { x: event.clientX, y: event.clientY }
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

	const handleComponentMouseEnter = (componentId: string) => {
		hoveredComponentId = componentId
	}

	const handleComponentMouseLeave = (componentId: string) => {
		if (hoveredComponentId === componentId) {
			hoveredComponentId = null
		}
	}
</script>
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	class="relative min-h-[400px]  lg:max-w-[70%] md:p-4 overflow-hidden mx-auto bg-surface-100 rounded-md"
	bind:this={viewport}
	on:wheel|preventDefault={handleWheel}
	on:mousedown={handleCanvasMouseDown}
	style="background-image: radial-gradient(circle, rgba(148,163,184,0.35) 1px, transparent 0); background-size: 24px 24px;"
>
	<div
		class="absolute top-4 right-4 flex flex-col gap-2 z-20"
		data-no-pan
	>
		<button
			type="button"
			class="button-neutral !px-2 !py-2 !rounded-md shadow"
			on:click|stopPropagation={zoomIn}
		>
			<Fa icon={faPlus} />
		</button>
		<button
			type="button"
			class="button-neutral !px-2 !py-2 !rounded-md shadow"
			on:click|stopPropagation={zoomOut}
		>
			<Fa icon={faMinus} />
		</button>
	</div>

	<div
		class="relative min-h-[400px] w-[1600px] space-y-8 isolate p-16"
		bind:this={container}
		
		style={`transform: translate(${panX}px, ${panY}px) scale(${scale}); transform-origin: 0 0;`}
	>
		{#if cycleError}
			<div class="md:max-w-5xl mx-auto">
				<Tip tipTheme="error" tipSize="lg" customIcon={faExclamationTriangle}>
					{cycleError}
				</Tip>
			</div>
		{/if}
		<svg class="absolute inset-0 z-10 pointer-events-none" width="100%" height="100%">
			{#each edgePaths as edge}
				<path
					d={edge.d}
					class={`opacity-80 transition-colors ${
						hoveredComponentId &&
						(edge.fromId === hoveredComponentId || edge.toId === hoveredComponentId)
							? 'text-primary-700'
							: 'text-primary-200'
					}`}
					stroke="currentColor"
					stroke-width="3"
					fill="none"
				/>
			{/each}
		</svg>
		<div class="w-full flex flex-col gap-16">
			{#each layers as layerGroup}
				<div class="flex flex-row gap-8 items-start flex-nowrap">
					{#each layerGroup.items as item (item.component.id)}
						<div
							animate:flip={{ duration: 300 }}
							class="relative m-2 max-w-[400px] flex-shrink-0"
							data-layer={layerById.get(item.component.id) ?? layerGroup.layer}
							data-component-id={item.component.id}
							on:mouseenter={() => handleComponentMouseEnter(item.component.id)}
							on:mouseleave={() => handleComponentMouseLeave(item.component.id)}
						>
							<div class="relative">
								<PipelineComponent
									component={item.component}
									cloneable={true}
									on:clone={(event) => cloneComponent(event.detail)}
									on:deleteComponent={(event) => deleteComponent(event.detail.oid)}
								/>
							</div>
							<!-- {#if item.orderIndex < orderedComponents.length - 1} -->
								<div
									class="my-4 mx-auto flex items-center justify-center
										relative
										before:absolute before:h-full before:w-1 before:-translate-x-1/2 before:left-1/2
										before:bg-surface-100-800-token before:-z-50 before:scale-y-[200%]
									"
								>
									<ComponentPopup {templateComponents} index={item.orderIndex + 1} />
								</div>
							<!-- {/if} -->
						</div>
					{/each}
				</div>
			{/each}
		</div>
		<!-- <div class="mx-auto flex items-center justify-center">
			<ComponentPopup {templateComponents} />
		</div> -->
	</div>
</div>
