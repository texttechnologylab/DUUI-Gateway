<script>
	import FunctionalTable from '$lib/svelte/components/FunctionalTable.svelte'
	import JsonDropdownInput from '$lib/svelte/components/Input/JsonDropdownInput.svelte'
	import { faPlus, faSave } from '@fortawesome/free-solid-svg-icons'
	import Text from '$lib/svelte/components/Input/TextInput.svelte'
	import Fa from 'svelte-fa'

	export let labels = ["a", "b", "c", "d", "e", "f", "g"]
	export let users = ["a", "b", "c", "d", "e", "f", "g"]

	export let rows = {
		row1: { groups: 'g1', members: [], labels: []},
		row2: { groups: 'g2', members: [], labels: ["a", "b"]}
	};

	export let saveCallback
	export let insertCallback
	export let deleteCallback

	let newGroupName

	const columns = { groups: "Groups" };


	let functionalColumns = [
		{
			label: 'Members',
			render: (key, row) => JsonDropdownInput, // Return the component reference
			props: (key, row) => ({
				dropdownList: users.map(v => v["email"]),
				target: row["members"],
				data: new Map(row["members"].map(v => [v, v]))
			})
		},
		{
			label: 'Labels',
			render: (key, row) => JsonDropdownInput, // Return the component reference
			props: (key, row) => ({
				dropdownList: labels,
				target: row["labels"],
				data: new Map(row["labels"].map(v => [v, v]))
			})
		},
		{
			label: 'Save',
			render: (key, row) => ({
				type: 'button',
				props: {
					class: "button-neutral !justify-center !border-none !rounded-none",
					onClick: saveCallback(key, row),
					icon: faSave
				}
			})
		},
		{
			label: 'Delete',
			render: (key, row) => ({
				type: 'button',
				props: {
					class: "button-neutral !justify-center !border-none !rounded-none",
					onClick: deleteCallback(key, row),
					icon: faSave
				}
			})
		}
	];
</script>

<FunctionalTable data={ rows } columns={ columns } { functionalColumns } />
<div style="display: flex; align-items: center; justify-content: flex-end;">
	<Text
		label=""
		style="self-center"
		name="newLabel"
		bind:value={newGroupName}
	/>
	<button
		class="button-neutral !justify-center !border-none !rounded-none"
		on:click={() => insertCallback(newGroupName, rows)}>
		<Fa icon={faPlus} />
	</button>
</div>