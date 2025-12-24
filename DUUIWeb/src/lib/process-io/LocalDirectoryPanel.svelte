<script lang="ts">
	import FolderStructure from '$lib/svelte/components/Input/FolderStructure.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'
	import { faWarning } from '@fortawesome/free-solid-svg-icons'
	import type { TreeViewNode } from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'

	export let name: string
	export let isMultiple: boolean
	export let value: string

	let lfs: TreeViewNode | undefined

	onMount(async () => {
		try {
			const response = await fetch('/api/settings/filtered-folder-structure', { method: 'GET' })
			if (response.ok) lfs = (await response.json()) as TreeViewNode
		} catch {}
	})
</script>

{#if Object.keys(lfs || {}).length > 0}
	<FolderStructure tree={lfs} label="Folder Picker" {name} {isMultiple} bind:value />
{:else}
	<Tip tipTheme="error" customIcon={faWarning}>
		The local drive is not available. Please select a different provider. To gain access to the local
		drive, please contact your administrator.
	</Tip>
{/if}
