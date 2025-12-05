<!--
	@component
	A responsive analogue to MobilePopup for desktop / large menus.

	- When the viewport width is >= `breakpoint`, the slotted content is rendered inline.
	- When the viewport width is < `breakpoint`, the content is placed into a popup
	  that is triggered by a "More" button with an ellipsis icon.

	Usage:
		<ResponsivePopup breakpoint={1024} popupId="pipeline-actions-desktop">
			<button class="button-menu">...</button>
			<button class="button-menu">...</button>
		</ResponsivePopup>
-->

<script lang="ts">
	import { onMount } from 'svelte'
	import { browser } from '$app/environment'
	import { faEllipsis } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import { popup, type PopupSettings } from '@skeletonlabs/skeleton'

	export let breakpoint: 'sm' | 'md' | 'lg' | 'xl' | '2xl' = 'lg'
	export let popupId: string = 'responsive-popup'

	let showInline = true

	const tailwindBreakpoints = {
		sm: 640,
		md: 768,
		lg: 1024,
		xl: 1280,
		'2xl': 1536
	} as const

	const dial: PopupSettings = {
		placement: 'bottom',
		target: popupId,
		event: 'click',
		middleware: { offset: 4 }
	}

	const updateMode = () => {
		if (!browser) return
		const minWidth = tailwindBreakpoints[breakpoint] ?? tailwindBreakpoints.lg
		// inline below breakpoint, menu at/above
		showInline = window.innerWidth >= minWidth
	}

	onMount(() => {
		if (!browser) return
		updateMode()
		const handler = () => updateMode()
		window.addEventListener('resize', handler)
		return () => window.removeEventListener('resize', handler)
	})
</script>

{#if showInline}
	<!-- Inline mode: render actions directly -->
	<!-- <div class="flex items-center"> -->
		<slot />
	<!-- </div> -->
{:else}
	<!-- Popup mode: actions in popup, triggered by More button -->
	<div data-popup={popupId}>
		<div class="popup-solid p-2 flex flex-col space-y-2">
			<slot />
		</div>
	</div>

	<button class="button-menu inline-flex items-center gap-2 px-4 border-x border-color" use:popup={dial}>
		<Fa icon={faEllipsis} />
		<span class="hidden md:inline">More</span>
	</button>
{/if}
