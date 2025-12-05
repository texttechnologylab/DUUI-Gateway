<script lang="ts">
	import { faWarning } from '@fortawesome/free-solid-svg-icons'
	import Popup from '$lib/svelte/components/Popup.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'

	export let tooltipVisible: boolean = false
	export let tooltipMessage: string = ''
	export let tipIcon = faWarning
	export let tipTheme: string = 'tertiary'
	export let tipIconSize: "sm" | "lg" | "md" | "xl" = "lg" 
	export let tipSize: "xs" | "sm" | "md" | "lg" = "md"
	export let autoPopupWidth: boolean = true
	export let showIcon: boolean = true

</script>

{#if tooltipVisible}
	<Popup {autoPopupWidth}>
		<svelte:fragment slot="trigger">
			<slot />
		</svelte:fragment>
		<svelte:fragment slot="popup">
			<Tip customIcon={tipIcon} {showIcon} tipTheme={tipTheme}>{tooltipMessage}{tipIconSize}{tipSize}</Tip>
		</svelte:fragment>
	</Popup>
{:else}
	<slot />
{/if}
