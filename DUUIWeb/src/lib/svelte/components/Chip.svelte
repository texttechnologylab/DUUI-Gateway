<script>
    import {faClose} from "@fortawesome/free-solid-svg-icons";
    import Fa from "svelte-fa";

    export let text = "";
</script>

<div class="tag">
  {#if $$slots["icon-left"]}
    <span class="mr-2 pt-1">
      <slot name="icon-left" />
    </span>
  {/if}

  <span>{#if text}{text}{/if}
    <slot />
  </span>

  {#if $$slots["icon-right"]}
    <span class="ml-2 pt-1">
      <slot name="icon-right" />
    </span>
  {/if}
</div>