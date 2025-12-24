<script lang="ts">
	import { hasFolderPicker, type IOProvider } from '$lib/duui/io.js'
	import FolderStructure from '$lib/svelte/components/Input/FolderStructure.svelte'
	import TextInput from '$lib/svelte/components/Input/TextInput.svelte'
	import { faRepeat } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import type { TreeViewNode } from '@skeletonlabs/skeleton'
	import _ from 'lodash'
	import { createEventDispatcher, onMount } from 'svelte'

	const { isEmpty } = _

	export let provider: IOProvider
	export let providerId: string
	export let selectedPaths: string
	export let name: string
	export let isMultiple: boolean
	export let userOid: string
	export let connections: Record<string, Record<string, unknown>>

	let remoteTree: TreeViewNode | undefined
	let fetchingTree = false

	const dispatch = createEventDispatcher<{ error: { message: string }; clearError: void }>()

	const fetchRemoteTree = async (reset: boolean) => {
		if (!hasFolderPicker(provider, true)) return
		if (!providerId) return
		if (!connections?.[provider.toLowerCase()]?.[providerId]) return

		fetchingTree = true
		const response = await fetch('/api/processes/folderstructure', {
			method: 'POST',
			body: JSON.stringify({ provider, user: userOid, reset, providerId })
		})
		fetchingTree = false

		if (response.ok) {
			remoteTree = (await response.json()) as TreeViewNode
			dispatch('clearError')
		} else {
			dispatch('error', { message: `Failed to fetch folder structure: ${response.statusText}` })
		}
	}

	onMount(async () => {
		if (providerId) await fetchRemoteTree(false)
	})
</script>

{#if !isEmpty(providerId) && hasFolderPicker(provider, true)}
	<div class="flex w-full">
		{#if !remoteTree}
			<div class="w">
				<TextInput
					label="Relative path"
					name="relativePath"
					bind:value={selectedPaths}
					error={selectedPaths === '/' ? 'Provide an empty path to select the root folder.' : ''}
				/>
			</div>
		{:else}
			<div class="w-11/12">
				<FolderStructure
					tree={remoteTree}
					label="Folder Picker"
					{name}
					{isMultiple}
					bind:value={selectedPaths}
				/>
			</div>
			<div class="w-1/12">
				<span class="form-label text-start">Refresh</span>
				<button
					class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
					on:click={() => fetchRemoteTree(true)}
				>
					<Fa icon={faRepeat} />
				</button>
			</div>
		{/if}
	</div>
{/if}
