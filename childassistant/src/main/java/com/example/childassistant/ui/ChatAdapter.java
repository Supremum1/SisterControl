package com.example.childassistant.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.childassistant.R;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;

    private final List<ChatItem> items;

    public ChatAdapter(List<ChatItem> items) {
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isUser() ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_chat_ai, parent, false);
        return new AiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = items.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(item.getText());
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(item.getText());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void add(ChatItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvUser);
        }

        void bind(String text) {
            textView.setText(text);
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvAi);
        }

        void bind(String text) {
            textView.setText(text);
        }
    }
}
