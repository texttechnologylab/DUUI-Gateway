<script lang="ts">
	  import type { IconDefinition } from "@fortawesome/free-solid-svg-icons"
    import Fa from "svelte-fa";

    export let text = "";
    export let leftIcon: IconDefinition | undefined = undefined;
    export let color: "primary" | "secondary" | "tertiary" | "error" = "primary";

    let classes = `variant-soft-${color} rounded-full py-0.5 px-2  flex items-center`
</script>

<div class={classes}>

  {#if leftIcon}
    <span>
      <Fa icon={leftIcon}/>
    </span>

    <!-- <span class="h-4 border-l border-primary-900 mx-2"></span> -->
  {/if}

  
  <span class="pt-0.5">
    {#if text}{text}{/if}
    <slot />
  </span>

  {#if $$slots["icon-right"]}
    <span class="ml-2 pt-1">
      <slot name="icon-right" />
    </span>
  {/if}
</div>