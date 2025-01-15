<!--
	@component
	A component for managing json like key value pairs.
-->
<script lang="ts">
	import { showConfirmationModal } from '$lib/svelte/utils/modal'
	import { faCheck, faClose, faPen, faPlus } from '@fortawesome/free-solid-svg-icons'
	import { getModalStore } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'
	import TextInput from './TextInput.svelte'
	import Dropdown from "$lib/svelte/components/Input/Dropdown.svelte";

	export let dropdownList: string[]
	export let data: Map<string, any> = new Map()
	export let target: string[]

	let edit: boolean = false
	let key: string = ''
	let value: string = ''
	export let label: string = ''

	const remove = async (key: string) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Parameter',
				message: `Please confirm the deletion of ${key}.`,
				textYes: 'Delete'
			},
			modalStore
		)

		if (confirm) {
			data.delete(key)
			data = data
			target = target.filter(item => item !== key)
		}
	}

	const create = () => {
		if (value === 'true') {
			data.set(key, true)
		} else if (value === 'false') {
			data.set(key, false)
		} else if (!isNaN(+value)) {
			data.set(key, +value)
		} else {
			data.set(key, value)
			target.push(key)
		}
		data = data
		edit = false
	}

	const modalStore = getModalStore()
	const clearParameters = async () => {
		const confirm = await showConfirmationModal(
			{
				title: 'Clear all Labels',
				message: 'Please confirm the deletion of ALL Labels.',
				textYes: 'Yes, clear'
			},
			modalStore
		)

		if (confirm) {
			data.clear()
			data = data
			target = []
		}
	}
</script>

<div class="label flex flex-col">
	{#if label}
		<span class="form-label">{label}</span>
	{/if}
	<div class=" space-y-4">
		<div class="flex items-center gap-2 mb-4">
			{#if !edit}
				<button
					class="button-neutral"
					on:click={() => {
						key = ''
						value = ''
						edit = true
					}}><Fa icon={faPlus} /><span>New</span></button
				>
				{#if data.size > 0}
					<button class="button-neutral" on:click={clearParameters}>
						<span>Clear All</span>
						<Fa icon={faClose} />
					</button>
				{/if}
			{:else}
				<div class="grid gap-1">
					<div class="flex-center-4">
						<Dropdown
								label="Add a Lable"
								offset={0}
								bind:value={key}
								on:change={(_) => {
									value = key
								}}
								style="button-menu px-4 py-2 !self-stretch"
								options={dropdownList}
						/>
						<button
							disabled={!key || !value || data[key]}
							class="pt-7 aspect-square rounded-full {key && value && !data[key]
								? 'hover:text-success-500 transition-colors'
								: 'opacity-50'}"
							on:click={create}><Fa icon={faCheck} size="lg" /></button
						>
					</div>
				</div>
			{/if}
		</div>
		{#if data.size === 0}
			<p>Select a value and confirm.</p>
		{/if}

		<div class="flex flex-wrap justify-start items-start gap-2">
			{#each data.entries() as [_key, _value]}
				<div class="input-wrapper p-4 min-w-[200px]">
					<div class="flex-center-4 justify-between">
						<p class="text-m font-bold">{_value}</p>
						<button
							class="rounded-full hover:text-error-500 transition-colors"
							on:click={() => remove(_key)}><Fa icon={faClose} size="lg" /></button
						>
					</div>
				</div>
			{/each}
		</div>
	</div>
</div>
