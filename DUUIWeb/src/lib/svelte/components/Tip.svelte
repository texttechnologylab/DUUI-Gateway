<script lang="ts">
	import { faLightbulb, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'

	export let showIcon: boolean = true
	export let transparent: boolean = true
	export let customIcon: IconDefinition = faLightbulb
	export let tipTheme: "primary" | "secondary" | "tertiary" | "error" | "success" | "surface"  = "primary"
</script>

{#if transparent}
	<div class={`flex gap-4 p-4 bordered-soft rounded-md variant-soft-${tipTheme} items-center my-4`}>
		{#if showIcon}
			<Fa icon={customIcon} size="lg" />
		{/if}
		<div class={showIcon ? 'border-l pl-4 border-color' : ''}>
			<slot />
		</div>
	</div>
{:else}
	<div class={`flex gap-4 p-4 bordered-soft rounded-md variant-soft-${tipTheme} items-center my-4`}>
		{#if showIcon}
			<Fa icon={customIcon} size="lg" />
		{/if}
		<div class={showIcon ? 'border-l pl-4 border-color' : ''}>
			<slot />
		</div>
	</div>
{/if}
