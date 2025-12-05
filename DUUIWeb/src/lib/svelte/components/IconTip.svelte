<script lang="ts">
	import { onMount } from 'svelte'
	import { browser } from '$app/environment'
	import { faInfoCircle, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'

	import Popup from '$lib/svelte/components/Popup.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'

	type TipTheme = "primary" | "secondary" | "tertiary" | "error" | "success" | "surface" | "warning"

	export let text: string = ''
	export let icon: IconDefinition = faInfoCircle
	export let tipTheme: TipTheme = 'primary'
	export let size: 'xs' | 'sm' | 'md' | 'lg' = 'sm'
	export let autoPopupWidth: boolean = true
	export let followCursor: boolean = false

	let triggerEl: HTMLElement | null = null
	let position: 'top' | 'bottom' = 'top'

	const tipThemeClass: Record<TipTheme, string> = {
		primary: 'variant-soft-primary',
		secondary: 'variant-soft-secondary',
		tertiary: 'variant-soft-tertiary',
		error: 'variant-soft-error',
		success: 'variant-soft-success',
		surface: 'variant-soft-surface',
		warning: 'variant-soft-warning'
	}

	const iconSizeByTipSize: Record<typeof size, 'sm' | 'md' | 'lg' | 'xl'> = {
		xs: 'sm',
		sm: 'sm',
		md: 'md',
		lg: 'lg'
	}

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

<Popup {position} {autoPopupWidth} {followCursor}>
	<svelte:fragment slot="trigger">
		<button
			type="button"
			class=""
			bind:this={triggerEl}
		>
			<Fa icon={icon} class={`${tipThemeClass[tipTheme]}`} size={iconSizeByTipSize[size]} />
		</button>
	</svelte:fragment>

	<svelte:fragment slot="popup">
		<Tip
			customIcon={icon}
			tipTheme={tipTheme}
			tipIconSize={iconSizeByTipSize[size]}
			tipSize={size}
			transparent={false}
		>
			{text}
		</Tip>
	</svelte:fragment>
</Popup>
