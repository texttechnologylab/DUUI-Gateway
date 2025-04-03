<script  lang="ts">
	import FunctionalTable from '$lib/svelte/components/FunctionalTable.svelte'
	import JsonDropdownInput from '$lib/svelte/components/Input/JsonDropdownInput.svelte'
	import { faPlus, faSave } from '@fortawesome/free-solid-svg-icons'
	import Text from '$lib/svelte/components/Input/TextInput.svelte'
	import Fa from 'svelte-fa'

	type Labels = {
		[labelId: string]: {
			label: string;
			driver: string;
			scope: string;
		}
	};

	export let labels: Labels = {
		"a": { label: "a", driver: "a", scope: "a" },
		"b": { label: "a", driver: "a", scope: "a" },
	}
	export let users = ["a", "b", "c", "d", "e", "f", "g"]

	export let rows = {
		row1: { groups: 'g1', members: [], labels: []},
		row2: { groups: 'g2', members: [], labels: ["a", "b"]}
	};

	export let saveCallback = (key, row) => {
		return () => {
			console.log(key, row)
		}
	}
	export let insertCallback = (newGroupName, rows) => {
		return () => {
			console.log(newGroupName, rows)
		}
	}
	export let deleteCallback = (key, row) => {
		return () => {
			console.log(key, row)
		}
	}

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

<div class="section-wrapper p-8 space-y-4 w-full">
	<div class="flex items gap-2 justify-start">
		<Text
			label="Add new group"
			style="self-center"
			name="newLabel"
			bind:value={newGroupName}
		/>
		<div class="mt-8 flex items-center gap-2">
			<button
				class=" button-neutral hover:!variant-soft-primary transition-300   "
				on:click={() => insertCallback(newGroupName, rows)}>
				<Fa icon={faPlus} />
				<span >Add</span>
			</button>
		</div>
	</div>
	<FunctionalTable data={ rows } columns={ columns } { functionalColumns } title="Groups" />
</div>

