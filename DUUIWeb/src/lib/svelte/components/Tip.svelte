<script lang="ts">
	import { faLightbulb, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'

	type TipTheme = "primary" | "secondary" | "tertiary" | "error" | "success" | "surface" | "warning"

	export let showIcon: boolean = true
	export let transparent: boolean = true
	export let customIcon: IconDefinition = faLightbulb
	export let tipTheme: TipTheme  = "primary"
	export let tipIconSize: "sm" | "lg" | "md" | "xl" = "lg" 
	export let tipSize: "xs" | "sm" | "md" | "lg" = "md"

	const tipThemeClass: Record<TipTheme, string> = {
		primary: 'variant-soft-primary',
		secondary: 'variant-soft-secondary',
		tertiary: 'variant-soft-tertiary',
		error: 'variant-soft-error',
		success: 'variant-soft-success',
		surface: 'variant-soft-surface',
		warning: 'variant-soft-warning'
	}

	$: sizeClasses =
		tipSize === 'xs'
			? 'gap-1 p-1 my-1 text-xs'
			: tipSize === 'sm'
				? 'gap-2 p-2 my-2 text-sm'
				: tipSize === 'lg'
					? 'gap-6 p-6 my-6 text-lg'
					: 'gap-4 p-4 my-4 text-base'
</script>

{#if transparent}
	<div class={`flex ${sizeClasses} bordered-soft rounded-md ${tipThemeClass[tipTheme]} items-center`}>
		{#if showIcon}
			<Fa icon={customIcon} size={tipIconSize} />
		{/if}
		<div class={showIcon ? 'border-l pl-4 border-color' : ''}>
			<slot />
		</div>
	</div>
{:else}
	<div class={`flex ${sizeClasses} popup-solid ${tipThemeClass[tipTheme]} items-center`}>
		{#if showIcon}
			<Fa icon={customIcon} size={tipIconSize} />
		{/if}
		<div class={showIcon ? 'border-l pl-4 border-color' : ''}>
			<slot />
		</div>
	</div>
{/if}
