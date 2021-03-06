package dev.wolveringer.bungeeutil.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

@Getter
public class ItemBuilder {
	@FunctionalInterface
	public static interface PostItemBuilder {
		public Item apply(ItemBuilder builder, Item in);
	}
	
	public static ItemBuilder create() {
		return new ItemBuilder();
	}

	public static ItemBuilder create(int id) {
		return new ItemBuilder(id);
	}

	public static ItemBuilder create(Item handle) {
		return new ItemBuilder(handle);
	}

	public static ItemBuilder create(Material m) {
		return create(m.getId());
	}

	private int id = -1;
	private int metaId = -1;
	private int amount = -1;
	private String name;
	private ArrayList<String> lore = new ArrayList<>();
	private boolean glow;
	private ArrayList<ClickListener> listener = new ArrayList<>();
	private Item handle;

	private List<PostItemBuilder> postListener = new ArrayList<>();
	
	public ItemBuilder() { }

	public ItemBuilder(int id) {
		this.id = id;
	}

	public ItemBuilder(Item handle) {
		this.handle = new Item(handle); //Copy
	}

	public ItemBuilder amount(int n) {
		this.amount = n;
		return this;
	}

	@SuppressWarnings("deprecation")
	public Item build() {
		Item i;
		if (this.handle != null) {
			if (this.id > 0) {
				this.handle.setTypeId(this.id);
			}
			if (this.metaId != -1) {
				this.handle.setDurability((short) this.metaId);
			}
			if(this.amount != -1) {
				this.handle.setAmount(this.amount);
			}
			i = this.handle;
		} else {
			Validate.validState(id >= 0, "Invalid item id ("+id+")");
			i = new Item(this.id, this.amount == -1 ? 1 : this.amount, (short) (this.metaId == -1 ? 0 : this.metaId));
		}
		if (!this.listener.isEmpty()) {
			List<ClickListener> copiedList = new ArrayList<>(this.listener);
			i = new ItemStack(i) {
				@Override
				public void click(Click c) {
					copiedList.forEach(e -> e.click(c));
				}
			};
		}
		if (this.name != null) {
			i.getItemMeta().setDisplayName(this.name);
		}
		if (!this.lore.isEmpty()) {
			i.getItemMeta().setLore(this.lore);
		}
		if (this.glow) {
			i.getItemMeta().setGlow(true);
		}
		for(PostItemBuilder postBuilder : postListener)
			postBuilder.apply(this, i);
		return i;
	}

	public ItemBuilder clearLore() {
		this.lore.clear();
		return this;
	}

	/*Spelling mistake */ //TODO Remove this
	@Deprecated
	public ItemBuilder durbility(int short_) {
		this.metaId = short_;
		return this;
	}

	public ItemBuilder durability(int short_) {
		this.metaId = short_;
		return this;
	}
	
	public ItemBuilder glow() {
		this.glow = true;
		return this;
	}

	public ItemBuilder glow(boolean flag) {
		this.glow = flag;
		return this;
	}

	public ItemBuilder id(int id) {
		this.id = id;
		return this;
	}

	public ItemBuilder listener(ClickListener run) {
		this.listener.add(run);
		return this;
	}

	public ItemBuilder listener(final Runnable run) {
		this.listener.add(click -> run.run());
		return this;
	}

	public ItemBuilder resetListener(){
		this.listener.clear();
		return this;
	}
	
	public ItemBuilder lore(String lore) {
		this.lore.add(lore);
		return this;
	}
	
	public ItemBuilder lore(String... lore) {
		this.lore.addAll(Arrays.asList(lore));
		return this;
	}

	public ItemBuilder lore(Collection<String> lore) {
		this.lore.addAll(lore);
		return this;
	}
	
	public ItemBuilder material(Material m) {
		this.id = m.getId();
		return this;
	}

	public ItemBuilder name(String name) {
		this.name = name;
		return this;
	}

	public ItemBuilder noName(){
		this.name = ChatColor.BLACK.toString();
		return this;
	}
	
	public ItemBuilder postListener(PostItemBuilder listener){
		this.postListener.add(listener);
		return this;
	}
	
	public ItemBuilder clone(){
		ItemBuilder _new = new ItemBuilder();
		_new.amount = this.amount;
		_new.glow = this.glow;
		_new.handle = this.handle == null ? null : new Item(this.handle);
		_new.id = this.id;
		_new.listener = this.listener;
		_new.lore = new ArrayList<>(this.lore);
		_new.name = this.name;
		_new.postListener = new ArrayList<>(this.postListener);
		_new.metaId = this.metaId;
		return _new;
	}
}
