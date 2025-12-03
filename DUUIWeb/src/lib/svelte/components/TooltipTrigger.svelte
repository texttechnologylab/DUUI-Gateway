<script lang="ts">
	import { faWarning } from '@fortawesome/free-solid-svg-icons'
	import Popup from '$lib/svelte/components/Popup.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'

	export let tooltipVisible: boolean = false
	export let tooltipMessage: string = ''
	export let tipIcon = faWarning
	export let tipTheme: string = 'tertiary'
	export let autoPopupWidth: boolean = true
</script>

{#if tooltipVisible}
	<Popup {autoPopupWidth}>
		<svelte:fragment slot="trigger">
			<slot />
		</svelte:fragment>
		<svelte:fragment slot="popup">
			<Tip customIcon={tipIcon} tipTheme={tipTheme}>{tooltipMessage}</Tip>
		</svelte:fragment>
	</Popup>
{:else}
	<slot />
{/if}
