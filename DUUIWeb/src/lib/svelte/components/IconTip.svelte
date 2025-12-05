<script lang="ts">
	import { onMount } from 'svelte'
	import { browser } from '$app/environment'
	import { faInfoCircle, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'

	import Popup from '$lib/svelte/components/Popup.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'

	type TipTheme = "primary" | "secondary" | "tertiary" | "error" | "success" | "surface" | "warning"

	export let text: string = ''
	export let showIcon: boolean = true
	export let icon: IconDefinition = faInfoCircle
	export let tipTheme: TipTheme = 'primary'
	export let size: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl' = 'sm'
	export let tipSize: 'xs' | 'sm' | 'md' | 'lg' | 'xl' = 'md'
	export let customTipSize = false
	export let autoPopupWidth: boolean = true
	export let followCursor: boolean = false
	export let buttonCallback: (() => void) | null = null
	let cursorOffsetY: number = 0
	let cursorOffsetX: number = 0

	let triggerEl: HTMLElement | null = null
	let position: 'top' | 'bottom' = 'top'

	const tipThemeClass: Record<TipTheme, string> = {
		primary: 'text-primary-700-200-token',
		secondary: 'text-secondary-700-200-token',
		tertiary: 'text-tertiary-700-200-token',
		error: 'text-error-700-200-token',
		success: 'text-success-700-200-token',
		surface: 'text-surface-700-200-token',
		warning: 'text-warning-700-200-token'
	}

	const iconSizeByTipSize: Record<typeof size, 'sm' | 'md' | 'lg' | '1.7x' | '2x'> = {
		xs: 'sm',
		sm: 'sm',
		md: 'md',
		lg: 'lg',
		xl: '1.7x',
		xxl: '2x'
	}

	$: actualTipSize = customTipSize ? tipSize : size
	
	$: triggerSizeClass =
		size === 'xs'
			? 'w-6 h-6'
			: size === 'sm'
			? 'w-8 h-8'
			: size === 'md'
			? 'w-10 h-10'
			: size === 'lg'
			? 'w-20 h-20'
			: size === 'xl'
			? 'w-14 h-14'
			: 'w-16 h-16' // xxl

	function updatePosition() {
		if (!browser || !triggerEl) return

		const rect = triggerEl.getBoundingClientRect()
		const viewportHeight = window.innerHeight || 0

		const spaceAbove = rect.top
		const spaceBelow = viewportHeight - rect.bottom

		// Prefer showing where there is more vertical space.
		position = spaceAbove < spaceBelow ? 'bottom' : 'top'
	}

	onMount(() => {
		if (!browser) return

		updatePosition()

		const handler = () => updatePosition()
		window.addEventListener('resize', handler)
		window.addEventListener('scroll', handler, true)

		return () => {
			window.removeEventListener('resize', handler)
			window.removeEventListener('scroll', handler, true)
		}
	})
</script>

<Popup {position} {autoPopupWidth} {followCursor} {cursorOffsetY} {cursorOffsetX}>
	<svelte:fragment slot="trigger">
		<button
			type="button"
			class={`inline-flex items-center justify-center rounded-full hover:${tipThemeClass[tipTheme]}`}
			bind:this={triggerEl}
			on:click={() => buttonCallback && buttonCallback()}
		>
			<Fa icon={icon} class={`${tipThemeClass[tipTheme]}`} size={iconSizeByTipSize[size]} />
		</button>
	</svelte:fragment>

	<svelte:fragment slot="popup">
		<Tip
			customIcon={icon}
			showIcon={showIcon}
			tipTheme={tipTheme}
			tipIconSize={iconSizeByTipSize[size]}
			tipSize={actualTipSize}
			transparent={false}
		>
			{text}
		</Tip>
	</svelte:fragment>
</Popup>
