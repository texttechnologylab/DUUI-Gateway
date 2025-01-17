<!--
	@component
	A Dropdown component where one option can be selected.
-->

<script lang="ts">
	import { equals, toTitleCase } from '$lib/duui/utils/text'
	import type { Placement } from '@floating-ui/dom'
	import { faCheck, faChevronDown, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import { ListBox, ListBoxItem, popup, type PopupSettings } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'

	export let label: string = ''
	export let name: string = label

	export const valueKey = "priority"
	type DropdownValue = object | string | number
	export let options: {[key: string]: DropdownValue}
	export let value: string | number

	const keyMapper = (key: string) => {
		return valueKey ? options[key][valueKey] : options[key]
	}

	const getName = (optionValue: string | number) => {
		return Object.entries(options)
			.filter(e => (valueKey ? e[1][valueKey] : e[1]) === optionValue)[0]
	}

	export let icon: IconDefinition = faChevronDown
	export let placement: Placement = 'bottom-end'

	export let offset: number = 4

	export let style: string = 'input-wrapper'
	export let rounded: string = 'rounded-md'
	export let border: string = 'border border-color'
	export let textAlign: string = 'text-start'

	const dropdown: PopupSettings = {
		event: 'click',
		target: name,
		placement: placement,
		closeQuery: '.listbox-item',
		middleware: {
			offset: offset
		}
	}
</script>

<div class="label flex flex-col md:min-w-[220px]">
	{#if label}
		<span class="form-label {textAlign}">{label} </span>
	{/if}
	<button
		class="flex items-center !justify-between gap-2 px-3 py-2 leading-6  {border} {rounded} {style}"
		use:popup={dropdown}
	>
		<span>{value === "" ? "" : toTitleCase('' + getName(value))}</span>
		<Fa {icon} />
	</button>
</div>

<div data-popup={name} class="fixed overflow-y-auto h64">
	<div class="popup-solid p-2 md:min-w-[220px] overflow-scroll max-h-96">
		<ListBox class="overflow-hidden" rounded="rounded-md" spacing="space-y-2">
			{#each Object.keys(options) as option}
				<ListBoxItem
					on:change
					bind:group={value}
					{name}
					value={keyMapper(option)}
					rounded="rounded-md"
					hover="hover:bg-surface-100-800-token"
					active="variant-filled-primary"
				>
					<svelte:fragment slot="lead">
						<Fa class={equals('' + value, '' + keyMapper(option)) ? '' : 'invisible'} icon={faCheck} />
					</svelte:fragment>
					{option}
				</ListBoxItem>
			{/each}
		</ListBox>
	</div>
</div>
