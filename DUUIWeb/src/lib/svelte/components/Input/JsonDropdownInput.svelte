<!--
	@component
	A component for managing json like key value pairs.
-->
<script lang="ts">
	import { showConfirmationModal } from '$lib/svelte/utils/modal'
	import { faCheck, faClose, faPlus, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import { getModalStore } from '@skeletonlabs/skeleton'
	import { Fa } from 'svelte-fa'
	import Dropdown from "$lib/svelte/components/Input/Dropdown.svelte";
	import Chip from "$lib/svelte/components/Chip.svelte";

	export let dropdownList: Map<string, any>
	export let data: Map<string, any> = new Map()
	export let target: string[] = []
	export let leadingIcon: IconDefinition | undefined = undefined

	let edit: boolean = false
	let key: string = ''
	let value: string = ''
	export let label: string = ''

	const remove = async (key: string, value: string) => {
		const confirm = await showConfirmationModal(
			{
				title: 'Delete Parameter',
				message: `Please confirm the deletion of ${value}.`,
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
			if (!data.has(key)) {
				data.set(key, value)
				target.push(key)
			}
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

	$: data = data
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
								label="Add a Label"
								capitalize={false}
								offset={0}
								bind:value={key}
								on:change={(_) => {
									if (dropdownList instanceof Map) {
										value = dropdownList.get(key) ?? key
									} else {
										value = key
									}
								}}
								style={"button-menu px-4 py-2 !self-stretch"}
								options={dropdownList}
						/>
						<div class="flex flex-col pt-6 gap-y-1">
							<button
								disabled={!key || !value || data[key]}
								class=" aspect-square rounded-full {key && value && !data[key]
									? 'hover:text-success-500 transition-colors'
									: 'opacity-50'}"
								on:click={() => create()}><Fa icon={faCheck} size="lg" /></button
							>
							<button
								class=" aspect-square rounded-full opacity-50 hover:text-error-500 transition-colors}"
								on:click={() => edit = false}><Fa icon={faClose} size="lg" /></button
							>
						</div>
					</div>
				</div>
			{/if}
		</div>

		<div class="flex flex-wrap justify-start items-start gap-2">
			{#each data.entries() as [_key, _value]}
				<Chip text={_value} leftIcon={leadingIcon}>
					<span slot="icon-right">
						<button class="hover:text-error-500 duration-300 transition-colors"
						on:click={() => remove(_key, _value)}>

							<Fa icon={faClose}  />

						</button>
					</span>
				</Chip>
			{/each}
		</div>
	</div>
</div>
