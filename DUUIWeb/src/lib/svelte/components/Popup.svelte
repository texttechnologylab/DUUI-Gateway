<!--
	@component
	A component representing a tooltip or menu that is visible when hovering the trigger.
	
	Uses Svelte slots for both the trigger and popup parts.
-->

<script lang="ts">
	import { onDestroy } from 'svelte'
	import { browser } from '$app/environment'

	export let arrow: boolean = true
	export let position: 'top' | 'bottom' | 'left' | 'right' = 'bottom'
	export let classes: string = ''
	export let autoPopupWidth: boolean = false
	// When true, position popup based on cursor instead of trigger element.
	export let followCursor: boolean = false

	let tooltipX = 0
	let tooltipY = 0
	let cursorVisible = false
	let popupEl: HTMLDivElement | null = null

	export let cursorOffsetY = 0
	export let cursorOffsetX = 0
	export let viewportMarginX = 8

	function updateFromEvent(event: MouseEvent) {
		if (!followCursor) return
		let x = event.clientX
		let y = event.clientY + cursorOffsetY

		if (browser && popupEl) {
			const viewportWidth = window.innerWidth || 0
			const popupWidth = popupEl.offsetWidth || 0
			const marginX = Math.max(0, viewportMarginX)
			const maxLeft = Math.max(marginX, viewportWidth - popupWidth - marginX)
			x = Math.max(marginX, Math.min(x, maxLeft))

			const viewportHeight = window.innerHeight || 0
			const popupHeight = popupEl.offsetHeight || 0
			const maxTop = Math.max(0, viewportHeight - popupHeight)
			y = Math.min(y, maxTop)
		}

		tooltipX = x
		tooltipY = y
	}

	function handleMouseEnter(event: MouseEvent) {
		if (!followCursor || !browser) return
		cursorVisible = true
		updateFromEvent(event)
		window.addEventListener('mousemove', updateFromEvent)
	}

	function handleMouseLeave() {
		if (!followCursor || !browser) return
		cursorVisible = false
		window.removeEventListener('mousemove', updateFromEvent)
	}

	onDestroy(() => {
		if (browser) {
			window.removeEventListener('mousemove', updateFromEvent)
		}
	})

	const popupWidth = autoPopupWidth ? "w-auto min-w-max" : ""
</script>

<!-- svelte-ignore a11y-no-static-element-interactions -->
<div
	class="group relative {classes}"
	style="display: inline;"
	on:mouseenter={handleMouseEnter}
	on:mouseleave={handleMouseLeave}
>
	<slot name="trigger" />
	{#if followCursor}
		{#if cursorVisible}
			<div
				class={`fixed z-[9999] pointer-events-none ${popupWidth}`}
				bind:this={popupEl}
				style={`left: ${tooltipX + cursorOffsetX}px; top: ${tooltipY + cursorOffsetY}px;`}
			>
				<div class="py-4 relative">
					<slot name="popup" />
				</div>
			</div>
		{/if}
	{:else if position === 'bottom'}
		<div
			class={`invisible group-hover:visible translate-y-4 transition-all origin-top opacity-0
                absolute left-1/2 -translate-x-1/2 top-full z-[9999]
                group-hover:translate-y-0 duration-300 group-hover:opacity-100 ${popupWidth}`}
		>
			<div class="py-4 relative">
				<slot name="popup" />
				{#if arrow}
					<div
						class="absolute rotate-45 top-2 w-2 h-2
                            left-1/2 translate-y-1/2
                            -translate-x-1/2 bg-surface-50-900-token border-l border-t border-color"
					/>
				{/if}
			</div>
		</div>
	{:else if position === 'top'}
		<div
			class={`invisible group-hover:visible -translate-y-4 transition-all origin-bottom opacity-0
                absolute left-1/2 -translate-x-1/2 bottom-full z-[9999]
                group-hover:translate-y-0 duration-300 group-hover:opacity-100 ${popupWidth}`}
		>
			<div class="py-4 relative">
				<slot name="popup" />

				{#if arrow}
					<div
						class="absolute rotate-45 bottom-2 w-2 h-2
                            left-1/2 -translate-y-1/2
                            -translate-x-1/2 bg-surface-50-900-token border-r border-b border-color"
					/>
				{/if}
			</div>
		</div>
	{:else if position === 'right'}
		<div
			class={`invisible group-hover:visible -translate-x-8 transition-all origin-bottom opacity-0
                absolute right-2 top-1/2 -translate-y-1/2 z-[9999]
                group-hover:translate-x-full duration-300 group-hover:opacity-100 ${popupWidth}`}
		>
			<div class="translate-x-4 relative">
				<slot name="popup" />

				{#if arrow}
					<div
						class="absolute rotate-45 top-1/2 w-2 h-2
                            left-0 -translate-x-1/2
                            -translate-y-1/2 bg-surface-50-900-token border-b border-l border-color"
					/>
				{/if}
			</div>
		</div>
	{:else if position === 'left'}
		<div
			class={`invisible group-hover:visible -translate-x-8 transition-all origin-bottom opacity-0
                absolute left-2 top-1/2 -translate-y-1/2 z-[9999]
                group-hover:-translate-x-full duration-300 group-hover:opacity-100 ${popupWidth}`}
		>
			<div class="-translate-x-4 relative">
				<slot name="popup" />

				{#if arrow}
					<div
						class="absolute rotate-45 top-1/2 w-2 h-2
                            right-0 translate-x-1/2
                            -translate-y-1/2 bg-surface-50-900-token border-t border-r border-color"
					/>
				{/if}
			</div>
		</div>
	{/if}
</div>
